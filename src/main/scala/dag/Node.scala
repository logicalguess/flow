package dag

import java.io.Serializable

import scala.collection.mutable.ArrayBuffer


case class Node(label: String, value: Any) extends Cloneable with Serializable {

  private val children = new ArrayBuffer[Node]
  private val parents = new ArrayBuffer[Node]
  var completed = false

  def addConnectorTo(node: Node) {
    if (!children.contains(node)) {
      children += node
    }
  }

  def removeConnectorTo(node: Node) {
    children -= node
  }

  def addConnectorFrom(node: Node) {
    if (!parents.contains(node)) {
      parents += node
    }
  }

  def removeConnectorFrom(node: Node) {
    parents -= node
  }

  def getChildren: ArrayBuffer[Node] = {
    children
  }

  def getChildLabels: ArrayBuffer[String] = {
    val retValue = new ArrayBuffer[String](children.size)
    for (child <- children.toList) retValue += child.label
    retValue
  }

  def getParents: ArrayBuffer[Node] = {
    parents
  }

  def getParentLabels: ArrayBuffer[String] = {
    val retValue = new ArrayBuffer[String](parents.size)
    for (parent: Node <- parents.toList) retValue += parent.label
    retValue
  }

  def isLeaf: Boolean = {
    children.size == 0
  }

  def isRoot: Boolean = {
    parents.size == 0
  }

  def isConnected: Boolean = {
    isRoot || isLeaf
  }

  @throws(classOf[CloneNotSupportedException])
  override def clone: AnyRef = {
    super.clone
  }

}