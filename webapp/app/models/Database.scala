package models

/**
 * Created by Jannis on 5/28/14.
 */
object Database {

  // credentials
  val url   = "jdbc:mysql://localhost/tippwm"
  val user  = "Jannis"
  val pw    = ""

  // for retrieving the data online
  //  val leagueShortcut = "WM-2014"
  val leagueShortcut = "test-wm"
  val leagueID = 676
  val season = 2014

  // Instantiate the driver
  val driver = "com.mysql.jdbc.Driver"
  Class.forName(driver).newInstance
}
