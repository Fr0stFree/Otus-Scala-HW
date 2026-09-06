package module5.zioeventsourcing

import zio.Chunk
import java.util.UUID

final case class AccountId(value: UUID) derives CanEqual
final case class CommandId(value: UUID) derives CanEqual

enum AccountStatus derives CanEqual:
  case New, Active, Frozen, Closed

final case class AccountState(
    owner: Option[String],
    balance: BigDecimal,
    status: AccountStatus
) derives CanEqual

object AccountState:
  val empty: AccountState = AccountState(None, BigDecimal(0), AccountStatus.New)

/**
 * IMPORTANT: this is intentionally NOT sealed.
 * StoreError is declared in Stores.scala, and Scala 3 does not allow a direct
 * child of a sealed trait to be declared in another source file.
 */
trait AppError extends Product with Serializable:
  def message: String

sealed trait AccountCommand:
  def accountId: AccountId
  def commandId: CommandId

object AccountCommand:
  final case class OpenAccount(
      accountId: AccountId,
      commandId: CommandId,
      owner: String
  ) extends AccountCommand

  final case class Deposit(
      accountId: AccountId,
      commandId: CommandId,
      amount: BigDecimal
  ) extends AccountCommand

  final case class Withdraw(
      accountId: AccountId,
      commandId: CommandId,
      amount: BigDecimal
  ) extends AccountCommand

  final case class Freeze(
      accountId: AccountId,
      commandId: CommandId,
      reason: String
  ) extends AccountCommand

  final case class Close(
      accountId: AccountId,
      commandId: CommandId
  ) extends AccountCommand

sealed trait AccountEvent derives CanEqual

object AccountEvent:
  final case class AccountOpened(owner: String) extends AccountEvent
  final case class MoneyDeposited(amount: BigDecimal) extends AccountEvent
  final case class MoneyWithdrawn(amount: BigDecimal) extends AccountEvent
  final case class AccountFrozen(reason: String) extends AccountEvent
  case object AccountClosed extends AccountEvent

enum DomainError(val message: String) extends AppError:
  case AlreadyOpened extends DomainError("account is already opened")
  case NotOpened extends DomainError("account is not opened")
  case AccountFrozen extends DomainError("account is frozen")
  case AccountClosed extends DomainError("account is closed")
  case AmountMustBePositive extends DomainError("amount must be > 0")
  case InsufficientFunds extends DomainError("insufficient funds")
  case BalanceMustBeZeroToClose extends DomainError("balance must be zero before closing")
  case OwnerMustBeNonEmpty extends DomainError("owner must be non-empty")

object Account:
  import AccountCommand.*
  import AccountEvent.*

  /** Pure command decision: current state + command => domain events or domain error. */
  def decide(
      state: AccountState,
      command: AccountCommand
  ): Either[DomainError, Chunk[AccountEvent]] =
    command match
      case OpenAccount(_, _, owner) =>
        if state.status != AccountStatus.New then Left(DomainError.AlreadyOpened)
        else if owner.trim.isEmpty then Left(DomainError.OwnerMustBeNonEmpty)
        else Right(Chunk(AccountOpened(owner.trim)))

      case Deposit(_, _, amount) =>
        validateMoneyOperation(state, amount).map(_ => Chunk(MoneyDeposited(amount)))

      case Withdraw(_, _, amount) =>
        for
          _ <- validateMoneyOperation(state, amount)
          _ <- Either.cond(state.balance >= amount, (), DomainError.InsufficientFunds)
        yield Chunk(MoneyWithdrawn(amount))

      case Freeze(_, _, reason) =>
        state.status match
          case AccountStatus.New    => Left(DomainError.NotOpened)
          case AccountStatus.Frozen => Right(Chunk.empty) // domain-level idempotency
          case AccountStatus.Closed => Left(DomainError.AccountClosed)
          case AccountStatus.Active => Right(Chunk(AccountFrozen(reason)))

      case Close(_, _) =>
        state.status match
          case AccountStatus.New    => Left(DomainError.NotOpened)
          case AccountStatus.Closed => Right(Chunk.empty)
          case _ if state.balance != 0 => Left(DomainError.BalanceMustBeZeroToClose)
          case _ => Right(Chunk(AccountClosed))

  /** Pure state transition used both after append and during replay. */
  def evolve(state: AccountState, event: AccountEvent): AccountState =
    event match
      case AccountOpened(owner)       => state.copy(owner = Some(owner), status = AccountStatus.Active)
      case MoneyDeposited(amount)     => state.copy(balance = state.balance + amount)
      case MoneyWithdrawn(amount)     => state.copy(balance = state.balance - amount)
      case AccountFrozen(_)           => state.copy(status = AccountStatus.Frozen)
      case AccountClosed              => state.copy(status = AccountStatus.Closed)

  private def validateMoneyOperation(
      state: AccountState,
      amount: BigDecimal
  ): Either[DomainError, Unit] =
    if amount <= 0 then Left(DomainError.AmountMustBePositive)
    else
      state.status match
        case AccountStatus.New    => Left(DomainError.NotOpened)
        case AccountStatus.Frozen => Left(DomainError.AccountFrozen)
        case AccountStatus.Closed => Left(DomainError.AccountClosed)
        case AccountStatus.Active => Right(())
