package dag

object DotFormatter {

  def format(links: List[(String, String)]): String = {
    val leftNodes = links.map(_._1).toSet
    val rightNodes = links.map(_._2).toSet
    val nodes = leftNodes.union(rightNodes)
    "digraph DAG {" +
      "   graph [fontsize=16 fontname=SEOptimist labelfontname=SEOptimist labelloc=\"t\" label=\"\" splines=true overlap=false ];\n" +
      "   node [fontsize=16  fontname=SEOptimist labelfontname=SEOptimist shape=Mrecord penwidth=0.25 style=filled fillcolor=lightgrey];\n" +
      "   edge [fontsize=12  fontname=SEOptimist labelfontname=SEOptimist penwidth=1.0 ];\n" +
      "   ratio = auto;\n" +
      "   bgcolor=\"transparent\";" +
      links.map(
        kv => "\"" + kv._1 + "\"->\"" + kv._2 + "\";"
      )
        .mkString("\n") //+
//      "{ rank=same;" + links.filter(kv => ! rightNodes.contains(kv._1)).map("\"" + _._1 + "\"").mkString(" ") + "}" +
//      "{ rank=same;" + links.filter(kv => ! leftNodes.contains(kv._2)).map("\"" + _._2 + "\"").mkString(" ") + "}" +
//      "}"

  }
}
