import axios from "axios";

const API_URL = "http://localhost:8080/api/legacy";

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