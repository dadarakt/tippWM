/**
 * Created by Jannis on 5/26/14.
 */

case class Team(name: String, id: Int, group: Char, iconUrl: String){
  override def toString = s"($name, $group)"
}

object Team {
  def allTeams: List[Team] = {
    // TODO make this happen
    List()
//    Retrieval.getAllTeams
  }
}
