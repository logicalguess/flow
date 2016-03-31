package dag

import flow.StatsCollector

object DotFormatter {

  def format(links: List[(String, String)], useStats: Boolean = false): String = {
    val leftNodes = links.map(_._1).toSet
    val rightNodes = links.map(_._2).toSet
    val nodes = leftNodes.union(rightNodes)

    val stats = StatsCollector.stats

    "digraph DAG {" +
      "   graph [fontsize=16 fontname=SEOptimist labelfontname=SEOptimist labelloc=\"t\" label=\"\" splines=true overlap=false ];\n" +
      "   node [fontsize=16  fontname=SEOptimist labelfontname=SEOptimist shape=Mrecord penwidth=0.25 style=filled fillcolor=lightgrey];\n" +
      "   edge [fontsize=12  fontname=SEOptimist labelfontname=SEOptimist penwidth=1.0 ];\n" +
      "   ratio = auto;\n" +
      "   bgcolor=\"transparent\";" +
      "   forcelabels=true;" +
      nodes.map(n =>  if (useStats) "\"" + n + "\" [label=\"" + n + "\\n" + stats.getOrElse(n, "NA") + " ms\", fontsize=14];" else "").mkString("\n") +
      links.map( _ match {
        case (from, to) => "\"" + from + "\"->\"" + to + "\";"
        }
      )
        .mkString("\n") //+
//      "{ rank=same;" + links.filter(kv => ! rightNodes.contains(kv._1)).map("\"" + _._1 + "\"").mkString(" ") + "}" +
//      "{ rank=same;" + links.filter(kv => ! leftNodes.contains(kv._2)).map("\"" + _._2 + "\"").mkString(" ") + "}" +
//      "}"

  }
}
