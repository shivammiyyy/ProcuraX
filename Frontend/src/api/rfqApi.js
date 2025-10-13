import axiosApi from "./axiosApi";

export const createRfq = (rfqData) => {
  return axiosApi.post('/rfq', rfqData, {
    headers: {
      'Content-Type': 'multipart/form-data',
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

export const getQuotationsForRfq = (rfqId) => {
  return axiosApi.get(`/quotation?rfqId=${rfqId}`);
};
