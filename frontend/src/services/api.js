import axios from "axios";

const API_BASE_URL =
  "https://legacymind-api.onrender.com/api/legacy";

// =====================================================
// ANALYZE LEGACY CODE
// =====================================================

export const getKnowledgeGraph = async (sourceCode) => {

  return axios.post(
    `${API_BASE_URL}/analyze`,
    {
      sourceCode
    }
  );
};