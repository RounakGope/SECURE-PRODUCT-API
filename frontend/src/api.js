import axios from "axios";

const api = axios.create({
  // Use the Vite proxy by default; override only if the API is hosted elsewhere.
  baseURL: import.meta.env.VITE_API_BASE_URL || "",
  headers: { "Content-Type": "application/json" },
  timeout: 15000
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("psa_token");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

export function getErrorMessage(error) {
  const data = error?.response?.data;
  if (typeof data === "string" && data.trim()) return data;
  if (data?.message) return data.message;
  if (data?.detail) return data.detail;
  if (data?.title) return data.title;
  if (error?.response?.status === 401) return "Session expired or credentials are invalid. Please log in again.";
  if (error?.response?.status === 403) return "You do not have permission to perform this action. Product changes may require ADMIN role.";
  if (error?.code === "ECONNABORTED") return "The request timed out. Please try again.";
  if (!error?.response) return "Could not reach the API. Check that Spring Boot is running on http://localhost:8080.";
  return `Request failed (${error.response.status}).`;
}

export default api;