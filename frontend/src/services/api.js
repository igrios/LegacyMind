import axios from "axios";

const API = axios.create({
  baseURL: "http://localhost:8080/api/legacy",
});

export const getGraph = (table) =>
  API.get(`/impact/graph/${table}`);