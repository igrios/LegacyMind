import axios from "axios";

const API_URL = process.env.REACT_APP_API_URL || "https://legacymind-api.onrender.com/api/legacy";

// 🔥 Knowledge Graph completo
export const getKnowledgeGraph = () => {

  return axios.get(
    `${API_URL}/knowledge-graph`
  );
};

// 🔵 Impact Graph viejo
export const getGraph = (table) => {

  return axios.get(
    `${API_URL}/impact/${table}`
  );
};
