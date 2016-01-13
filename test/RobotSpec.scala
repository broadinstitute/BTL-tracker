/**
  * Created by nnovod on 1/13/16.
  */

import models.Robot.RobotType
import models.Robot
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import ScanFileOpers._

import scala.concurrent.Await

@RunWith(classOf[JUnitRunner])
class RobotSpec extends TestSpec with TestConfig {
	"The robot" must {
		"make instructions" in {
			val robot = Robot(RobotType.HAMILTON)
			val res = Await.result(robot.makeABPlate("AB-R1", "AB-P1", "NN-15096179"), d3secs)
			println(res)
		}
	}
}
