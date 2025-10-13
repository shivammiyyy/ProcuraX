import React, { useState } from 'react';
import axios from 'axios';
import { useAuth } from '../../context/AuthContext';

const QuotationForm = ({ rfqId }) => {
  const { user, loading } = useAuth();

  const [formData, setFormData] = useState({
    price: '',
    deliveryTimeDays: '',
    compliance: {
      ISO_Certification: false,
      Material_Grade: 'A',
      Environmental_Standards: false,
      Document_Submission: false,
    },
    file: null,
  });

  const [submitting, setSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');
  const [successMsg, setSuccessMsg] = useState('');

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({ ...prev, [name]: value }));
  };

  const handleFileChange = (e) => {
    setFormData(prev => ({ ...prev, file: e.target.files[0] }));
  };

  const handleComplianceChange = (e) => {
    const { name, type, checked, value } = e.target;
    setFormData(prev => ({
      ...prev,
      compliance: {
        ...prev.compliance,
        [name]: type === 'checkbox' ? checked : value,
      },
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    setErrorMsg('');
    setSuccessMsg('');

    if (!formData.file) {
      setErrorMsg('Please upload a file.');
      setSubmitting(false);
      return;
    }

    try {
      const data = new FormData();
      data.append('rfqId', rfqId);
      data.append('price', formData.price);
      data.append('deliveryTimeDays', formData.deliveryTimeDays);
      data.append('compliance', JSON.stringify(formData.compliance));
      data.append('file', formData.file);

      const res = await axios.post('/api/v0/quotation', data, {
        headers: {
          'Content-Type': 'multipart/form-data',
          Authorization: `Bearer ${user?.token}`,
        },
      });

      setSuccessMsg(res.data.message || 'Quotation created successfully!');
      setFormData({
        price: '',
        deliveryTimeDays: '',
        compliance: {
          ISO_Certification: false,
          Material_Grade: 'A',
          Environmental_Standards: false,
          Document_Submission: false,
        },
        file: null,
      });
    } catch (err) {
      setErrorMsg(err.response?.data?.message || 'Failed to create quotation.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <form
      onSubmit={handleSubmit}
      className="bg-white p-8 rounded shadow-md w-full max-w-lg"
      encType="multipart/form-data"
    >
      <h2 className="text-2xl font-bold mb-6">Submit Quotation</h2>

      {errorMsg && <div className="bg-red-100 text-red-700 p-2 mb-4 rounded">{errorMsg}</div>}
      {successMsg && <div className="bg-green-100 text-green-700 p-2 mb-4 rounded">{successMsg}</div>}

      <div className="mb-4">
        <label className="block mb-1 font-semibold">Price</label>
        <input
          type="number"
          name="price"
          value={formData.price}
          onChange={handleChange}
          required
          className="w-full border rounded px-3 py-2"
        />
      </div>

      <div className="mb-4">
        <label className="block mb-1 font-semibold">Delivery Time (Days)</label>
        <input
          type="number"
          name="deliveryTimeDays"
          value={formData.deliveryTimeDays}
          onChange={handleChange}
          required
          className="w-full border rounded px-3 py-2"
        />
      </div>

      <h3 className="text-lg font-semibold mb-2">Compliance Details</h3>

      <div className="mb-2">
        <label className="flex items-center gap-2">
          <input
            type="checkbox"
            name="ISO_Certification"
            checked={formData.compliance.ISO_Certification}
            onChange={handleComplianceChange}
          />
          ISO Certification
        </label>
      </div>

      <div className="mb-2">
        <label className="block mb-1 font-semibold">Material Grade</label>
        <select
          name="Material_Grade"
          value={formData.compliance.Material_Grade}
          onChange={handleComplianceChange}
          className="w-full border rounded px-3 py-2"
        >
          <option value="A+">A+</option>
          <option value="A">A</option>
          <option value="B">B</option>
        </select>
      </div>

      <div className="mb-2">
        <label className="flex items-center gap-2">
          <input
            type="checkbox"
            name="Environmental_Standards"
            checked={formData.compliance.Environmental_Standards}
            onChange={handleComplianceChange}
          />
          Environmental Standards
        </label>
      </div>

      <div className="mb-4">
        <label className="flex items-center gap-2">
          <input
            type="checkbox"
            name="Document_Submission"
            checked={formData.compliance.Document_Submission}
            onChange={handleComplianceChange}
          />
          Document Submission
        </label>
      </div>

      <div className="mb-6">
        <label className="block mb-1 font-semibold">Upload File</label>
        <input
          type="file"
          name="file"
          accept=".pdf,.doc,.docx"
          onChange={handleFileChange}
          required
          className="w-full border rounded px-3 py-2"
        />
      </div>

      <button
        type="submit"
        disabled={submitting}
        className="w-full bg-green-600 hover:bg-green-700 text-white font-semibold py-2 rounded"
      >
        {submitting ? 'Submitting...' : 'Submit Quotation'}
      </button>
    </form>
  );
};

export default QuotationForm;
