import axiosApi from "./axiosApi";

export const createRfq = (rfqData) => {
    
    return axiosApi.post('/rfqs', rfqData);
};

export const getRfqs = () => {
    return axiosApi.get('/rfqs');
};

export const getRfqById = (id) => {
    return axiosApi.get(`/rfqs/${id}`);
};

export const updateRfq = (id, rfqData) => {
    return axiosApi.put(`/rfqs/${id}`, rfqData);
};
export const deleteRfq = (id) => {
    return axiosApi.delete(`/rfqs/${id}`);
};

