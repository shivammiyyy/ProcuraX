import axiosApi from "./axiosApi";

export const createRfq = (rfqData) => {
    return axiosApi.post('/rfq', rfqData, {
         headers: {
      Authorization: `Bearer ${localStorage.getItem('token')}`,
      // ❌ Do not manually set Content-Type; Axios does it automatically for FormData
    },
    });
};

export const getRfqs = () => {
    return axiosApi.get('/rfq');
};

export const getRfqById = (id) => {
    return axiosApi.get(`/rfq/${id}`);
};

export const updateRfq = (id, rfqData) => {
    return axiosApi.put(`/rfq/${id}`, rfqData);
};

export const deleteRfq = (id) => {
    return axiosApi.delete(`/rfq/${id}`);
};



export const getQuotationByRFQId = (rfqId) => {
  return axiosApi.get(`/rfq/${rfqId}/quotations`);
};