import axiosApi from "./axiosApi";

export const createContract = (formData) => {
  return axiosApi.post('/contracts', formData);
};

export const getContracts = () => {
  return axiosApi.get('/contracts' );
}

export const getContractById = (id) => {
  return axiosApi.get(`/contracts/${id}`);
}

export const updateContract = (id, updatedData) => {
  return axiosApi.put(`/contracts/${id}`, updatedData);
}

