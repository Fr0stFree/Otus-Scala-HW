package module1

import org.scalatest.flatspec.AnyFlatSpec
import ru.otus.module1.collections2.{Cons, Nil, ToyList}

class ToyListSpec extends AnyFlatSpec{

  "ToyList empty" should "return empty ToyList" in {
    val l1 = ToyList.empty[Int]
    assert(l1.isEmpty)
  }

  "apply from 3 ints" should "create non empty ToyList of this 3 ints" in {
    val l1 = Cons(1, Cons(2, Cons(3, Nil)))
    assert(ToyList(1, 2, 3) === l1)
  }

  "prepend" should "add 1 element to the head of the ToyList" in{
    val l1  = ToyList.empty[Int]
    assert(1 :: l1 === ToyList(1))
  }

  "reverse" should "return new ToyList in reverse order" in {
    val l1 = ToyList(1, 2, 3)
    val result = ToyList(3, 2, 1)
    assert(l1.reverse === result)
  }

  "concat of 2 ToyList" should "return new ToyList with elements from both" in {
    val l1 = ToyList(1, 2, 3)
    val l2 = ToyList(4, 5, 6)
    val result = ToyList(1, 2, 3, 4, 5, 6)
    assert(l1.concat(l2) === result)
  }

  "flatMap" should "apply transformation correctly" in {
    val l1 = ToyList(1, 2, 3)
    val result = ToyList(2, 3, 4)
    assert(l1.flatMap(v => ToyList(v + 1)) === result)
  }

  "map" should "apply transformation correctly" in {
    val l1 = ToyList(1, 2, 3)
    val result = ToyList(2, 3, 4)
    assert(l1.map(v => v + 1) === result)
  }
  
  "fold" should "collapse ToyList as expected" in {
    val l1 = ToyList(1, 2, 3)
    val result = 6
    assert(l1.foldLeft(0)((acc, el) => acc + el) === result)
  }


}
