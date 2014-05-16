package models

/**
 * Created by jannis on 5/16/14.
 * Models a participant in the game.
 */
case class Player(firstName: String, lastName: String, nickName: String, password: String) {
  val id = (firstName + lastName + nickName).hashCode

  override def toString = s"$firstName '$nickName' $lastName"
}

object Player {
  def all: List[Player] = Nil
  def create(firstName: String, lastName: String, nickName: String, password: String) {}
  def delete(id: Int) {} //TODO make sure to use the password here!
}
