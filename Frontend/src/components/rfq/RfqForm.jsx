import React, { useState } from 'react';
import { createRfq } from '../../api/rfqApi';
import { useNavigate } from 'react-router-dom';

const RfqForm = () => {
  const [formData, setFormData] = useState({
    title: '',
    description: '',
    requestType: 'RFQ',
    budget: '',
    deadline: '',
    category: '',
  });
  const navigate = useNavigate();

  const [attachments, setAttachments] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  const handleChange = (e) => {
    setFormData((prev) => ({ ...prev, [e.target.name]: e.target.value }));
  };

  const handleFileChange = (e) => {
    setAttachments([...e.target.files]);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    setLoading(true);

    try {
      const data = new FormData();
      Object.entries(formData).forEach(([key, value]) => data.append(key, value));
      attachments.forEach((file) => data.append('attachment', file));

      const response = await createRfq(data);
      setSuccess(response.data.message || 'RFQ created successfully!');
      setFormData({
        title: '',
        description: '',
        requestType: 'RFQ',
        budget: '',
        deadline: '',
        category: '',
      });
      setAttachments([]);
      navigate('/')

    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create RFQ. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="bg-white p-8 rounded shadow-md w-full max-w-lg mx-auto">
      <h2 className="text-2xl font-bold mb-6">Create RFQ / RFP</h2>
      {error && <div className="bg-red-100 text-red-700 p-2 mb-4 rounded">{error}</div>}
      {success && <div className="bg-green-100 text-green-700 p-2 mb-4 rounded">{success}</div>}
      <div className="mb-4">
        <label htmlFor="title" className="block mb-1 font-semibold">Title</label>
        <input
          id="title"
          name="title"
          type="text"
          className="w-full border rounded px-3 py-2"
          value={formData.title}
          onChange={handleChange}
          disabled={loading}
          required
        />
      </div>
      <div className="mb-4">
        <label htmlFor="description" className="block mb-1 font-semibold">Description</label>
        <textarea
          id="description"
          name="description"
          className="w-full border rounded px-3 py-2"
          rows={4}
          value={formData.description}
          onChange={handleChange}
          disabled={loading}
          required
        />
      </div>
      <div className="mb-4">
        <label htmlFor="requestType" className="block mb-1 font-semibold">Request Type</label>
        <select
          id="requestType"
          name="requestType"
          className="w-full border rounded px-3 py-2"
          value={formData.requestType}
          onChange={handleChange}
          disabled={loading}
          required
        >
          <option value="RFQ">RFQ (Request for Quotation)</option>
          <option value="RFP">RFP (Request for Proposal)</option>
        </select>
      </div>
      <div className="mb-4">
        <label htmlFor="budget" className="block mb-1 font-semibold">Budget (₹)</label>
        <input
          id="budget"
          name="budget"
          type="number"
          className="w-full border rounded px-3 py-2"
          value={formData.budget}
          onChange={handleChange}
          disabled={loading}
          required
        />
      </div>
      <div className="mb-4">
        <label htmlFor="category" className="block mb-1 font-semibold">Category</label>
        <select
          id="category"
          name="category"
          className="w-full border rounded px-3 py-2"
          value={formData.category}
          onChange={handleChange}
          disabled={loading}
          required
        >
          <option value="">Select a category</option>
          <option value="Office Supplies">Office Supplies</option>
          <option value="IT Hardware">IT Hardware</option>
          <option value="Raw Materials">Raw Materials</option>
        </select>
      </div>
      <div className="mb-4">
        <label htmlFor="deadline" className="block mb-1 font-semibold">Deadline</label>
        <input
          id="deadline"
          name="deadline"
          type="date"
          className="w-full border rounded px-3 py-2"
          value={formData.deadline}
          onChange={handleChange}
          disabled={loading}
          required
        />
      </div>
      <div className="mb-6">
        <label htmlFor="attachment" className="block mb-1 font-semibold">Attachments (optional)</label>
        <input
          id="attachment"
          name="attachment"
          type="file"
          multiple
          onChange={handleFileChange}
          disabled={loading}
          className="w-full border rounded px-3 py-2"
        />
      </div>
      <button
        type="submit"
        disabled={loading}
        className="w-full bg-green-600 hover:bg-green-700 text-white font-semibold py-2 rounded"
      >
        {loading ? 'Submitting...' : 'Submit RFQ'}
      </button>
    </form>
  );
};

export default RfqForm;