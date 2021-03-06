package dag

import java.io.{File, FileInputStream, Serializable}
import java.util

import scala.collection.mutable.{ArrayBuffer, ListBuffer}


case class DAG(name: String,
               nodes: List[Node],
               connectors: List[Connector]
                     ) extends Cloneable with Serializable {


  val nodeMap = new util.HashMap[String, Node]
  val nodeListInner = new ListBuffer[Node]


  nodes.foreach(addNode)
  connectors.foreach(addConnector)


  def getLabels: util.Set[String] = {
    nodeMap.keySet
  }

  def getNodes: ListBuffer[Node] = {
    nodeListInner
  }

  def getRoots: ListBuffer[Node] = {
    getNodes.filter(x => x.isRoot)
  }

  def getLeaves: ListBuffer[Node] = {
    getNodes.filter(x => x.isLeaf)
  }

  def addNode(node: Node): Node = {
    var retValue: Node = null
    if (this.nodeMap.containsKey(node.label)) {
      retValue = nodeMap.get(node.label)
    }
    else {
      retValue = node
      this.nodeMap.put(node.label, retValue)
      this.nodeListInner += node
    }
    retValue
  }

  @throws(classOf[CycleDetectedException])
  def addConnector(from: String, to: String) {
    val v1 = addNode(nodeMap.get(from))
    val v2 = addNode(nodeMap.get(to))
    addConnector(v1, v2)
  }

  @throws(classOf[CycleDetectedException])
  def addConnector(connector: Connector): Unit = {
    addConnector(connector.from, connector.to)
  }

  @throws(classOf[CycleDetectedException])
  def addConnector(from: Node, to: Node) {
    from.addConnectorTo(to)
    to.addConnectorFrom(from)
    val cycle = CycleDetector.introducesCycle(to)
    if (cycle != null) {
      removeConnector(from, to)
      val msg: String = "Connector between \'" + from + "\' and \'" + to + "\' introduces to cycle in the workflow"
      throw new CycleDetectedException(msg, cycle)
    }
  }

  def removeConnector(from: String, to: String) {
    val v1: Node = addNode(nodeMap.get(from))
    val v2: Node = addNode(nodeMap.get(from))
    removeConnector(v1, v2)
  }

  def removeConnector(from: Node, to: Node) {
    from.removeConnectorTo(to)
    to.removeConnectorFrom(from)
  }

  def getNode(label: String): Node = {
    nodeMap.get(label)
  }

  def hasConnector(label1: String, label2: String): Boolean = {
    val v1 = getNode(label1)
    val v2 = getNode(label2)
    v1.getChildren.contains(v2)
  }

  def getChildLabels(label: String): ArrayBuffer[String] = {
    val node = getNode(label)
    node.getChildLabels
  }

  def getParentLabels(label: String): ArrayBuffer[String] = {
    val node = getNode(label)
    node.getParentLabels
  }

  @throws(classOf[CloneNotSupportedException])
  override def clone: AnyRef = {
    super.clone

  }

  def isConnected(label: String): Boolean = {
    val node = this.getNode(label)
    node.isConnected
  }

  def getSuccessorLabels(label: String): util.List[String] = {
    val node = getNode(label)
    var retValue: util.List[String] = new util.ArrayList[String]
    if (node.isLeaf) {
      retValue = new util.ArrayList[String](1)
      retValue.add(label)
    }
    else {
      retValue = TopologicalSorter.sort(node)
    }
    retValue
  }
}

object DAG {

  def dotFormatDiagram(graph: DAG, useStats: Boolean = false): String = {
    val edges: List[(String, String)] = graph.connectors.map(c => (c.from, c.to))
    //val nodes: List[(String, String)] = graph.nodes.map(n => (n.label, n.label))
    DotFormatter.format(edges, useStats)
  }

  def apply(name: String, nodes: List[String]*): DAG = {
    val ns = for {
      node <- nodes
    } yield Node(node.head)
    val cs = for {
      node: List[String] <- nodes
      deps =  node.tail
      dep <- deps
    } yield Connector(dep, node.head)
    new DAG(name, ns.toList, cs.toList)
  }
}