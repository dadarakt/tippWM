/**
 * Created by Jannis on 5/22/14.
 */

import Data._
import java.util.Date
import org.scalatest.FunSuite

class PointsTest extends FunSuite{

  val match1 = new Match("Argentinien",764,"Bosnien und Herzegowina",2671,'F', new Date(),"Rio de Janeiro",915,"Maracanã",27038,14053,1,"Vorrunde",2,1,true)
  val tipp1 = new Tipp(27038,"KasperZuWursthausen", 2,1)
  val tipp2 = new Tipp(27038, "chae", 3,2)
  val tipp3 = new Tipp(27038, "",5,0)
  val tipp4 = new Tipp(27038, "",1,1)

  val match2 = new Match("Argentinien",764,"Bosnien und Herzegowina",2671,'F', new Date(),"Rio de Janeiro",915,"Maracanã",27038,14053,1,"Vorrunde",1,1,true)
  val tipp5 = new Tipp(27038,"KasperZuWursthausen", 1,1)
  val tipp6 = new Tipp(27038, "chae", 0,0)
  val tipp7 = new Tipp(27038, "",3,3)
  val tipp8 = new Tipp(27038, "",1,0)

  test("Test a spot-on tipp") {
    assert(Tipp.getPointsForTipp(match1,tipp1) === 4)
    assert(Tipp.getPointsForTipp(match2,tipp5) === 4)
  }

  test("Test a right diff tipp") {
    assert(Tipp.getPointsForTipp(match1, tipp2) === 3)
    assert(Tipp.getPointsForTipp(match2, tipp6) === 2) // this is for equal goals
  }

  test("Test a right tendency tipp") {
    assert(Tipp.getPointsForTipp(match1, tipp3) === 2)
    assert(Tipp.getPointsForTipp(match2, tipp7) === 2)
  }

  test("Test a wrong tipp") {
    assert(Tipp.getPointsForTipp(match1, tipp4) === 0)
    assert(Tipp.getPointsForTipp(match2, tipp8) === 0)
  }
}
