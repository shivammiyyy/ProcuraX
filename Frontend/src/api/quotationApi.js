import axiosApi from "./axiosApi";

export const createQuotation = (quotationData) => {
    return axiosApi.post('/quotation', quotationData);
};

export const getQuotations = () => {
    return axiosApi.get('/quotation');
};

export const getQuotationById = (id) => {
    return axiosApi.get(`/quotation/${id}`);
};

export const updateQuotation = (id, quotationData) => {
    return axiosApi.put(`/quotation/${id}`, quotationData);
};

export const deleteQuotation = (id) => {
    return axiosApi.delete(`/quotation/${id}`);
};
