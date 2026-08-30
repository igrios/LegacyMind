import dagre from "dagre";

const NODE_WIDTH = 220;
const NODE_HEIGHT = 80;

export default function getLayoutedElements(
  nodes,
  edges
) {

  const dagreGraph = new dagre.graphlib.Graph();
  dagreGraph.setDefaultEdgeLabel(() => ({}));

  // 🔥 Dirección del layout
  // TB = top-bottom
  // LR = left-right

  dagreGraph.setGraph({

    rankdir: "TB",

    nodesep: 140,

    ranksep: 190,

    marginx: 50,

    marginy: 50
  });

  // 🔵 Registrar nodos
  nodes.forEach((node) => {

    dagreGraph.setNode(node.id, {

      width: NODE_WIDTH,

      height: NODE_HEIGHT
    });
  });

  // 🔗 Registrar edges
  edges.forEach((edge) => {

    dagreGraph.setEdge(
      edge.source,
      edge.target
    );
  });

  // ⚡ Ejecutar layout
  dagre.layout(dagreGraph);

  // 🔥 Aplicar posiciones calculadas
  const layoutedNodes = nodes.map((node) => {

    const position =
      dagreGraph.node(node.id);

    return {

      ...node,

      position: {

        x:
          position.x - NODE_WIDTH / 2,

        y:
          position.y - NODE_HEIGHT / 2
      }
    };
  });

  return {

    nodes: layoutedNodes,

    edges
  };
}
