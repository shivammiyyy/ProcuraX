import axiosApi from "./axiosApi";

export const createContract = (formData) => {
  return axiosApi.post('/contract', formData); // Singular 'contract' to match backend
};

export const getContracts = () => {
  return axiosApi.get('/contract');
};

export const getContractById = (id) => {
  return axiosApi.get(`/contract/${id}`);
};

export const updateContract = (id, updatedData) => {
  return axiosApi.put(`/contract/${id}`, updatedData);
};
