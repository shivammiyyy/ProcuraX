import React, { useState } from "react";
import { createRfq } from "../../api/rfqApi";
import { useNavigate } from "react-router-dom";

const RfqForm = () => {
  const [formData, setFormData] = useState({
    title: "",
    description: "",
    requestType: "RFQ",
    budget: "",
    deadline: "",
    category: "",
  });
  const [attachments, setAttachments] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const navigate = useNavigate();

  const handleChange = (e) => {
    setFormData((prev) => ({ ...prev, [e.target.name]: e.target.value }));
  };

  const handleFileChange = (e) => {
    setAttachments([...e.target.files]);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setSuccess("");
    setLoading(true);

    try {
      const data = new FormData();
      Object.entries(formData).forEach(([k, v]) => data.append(k, v));
      attachments.forEach((file) => data.append("attachment", file));

      const res = await createRfq(data);
      setSuccess(res.data.message || "RFQ created successfully!");
      navigate("/");
    } catch (err) {
      setError(err.response?.data?.message || "Failed to create RFQ.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-3xl mx-auto bg-white shadow-lg rounded-2xl border border-slate-200 p-8 transition-all hover:shadow-xl">
      <h2 className="text-3xl font-bold text-slate-800 mb-6 text-center">
        Create <span className="text-indigo-600">RFQ / RFP</span>
      </h2>

      {error && <p className="bg-red-100 text-red-700 p-3 rounded mb-4">{error}</p>}
      {success && <p className="bg-emerald-100 text-emerald-700 p-3 rounded mb-4">{success}</p>}

      <form onSubmit={handleSubmit} className="space-y-5">
        <div>
          <label className="block text-sm font-semibold text-slate-700 mb-1">Title</label>
          <input
            name="title"
            type="text"
            className="w-full border rounded-lg px-3 py-2 focus:ring-2 focus:ring-indigo-400 outline-none"
            value={formData.title}
            onChange={handleChange}
            required
          />
        </div>

        <div>
          <label className="block text-sm font-semibold text-slate-700 mb-1">Description</label>
          <textarea
            name="description"
            rows={4}
            className="w-full border rounded-lg px-3 py-2 focus:ring-2 focus:ring-indigo-400 outline-none"
            value={formData.description}
            onChange={handleChange}
            required
          />
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-1">Request Type</label>
            <select
              name="requestType"
              className="w-full border rounded-lg px-3 py-2 focus:ring-2 focus:ring-indigo-400"
              value={formData.requestType}
              onChange={handleChange}
            >
              <option value="RFQ">RFQ (Quotation)</option>
              <option value="RFP">RFP (Proposal)</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-1">Category</label>
            <select
              name="category"
              className="w-full border rounded-lg px-3 py-2 focus:ring-2 focus:ring-indigo-400"
              value={formData.category}
              onChange={handleChange}
              required
            >
              <option value="">Select Category</option>
              <option value="Office Supplies">Office Supplies</option>
              <option value="IT Hardware">IT Hardware</option>
              <option value="Raw Materials">Raw Materials</option>
            </select>
          </div>
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-1">Budget (₹)</label>
            <input
              name="budget"
              type="number"
              className="w-full border rounded-lg px-3 py-2 focus:ring-2 focus:ring-indigo-400"
              value={formData.budget}
              onChange={handleChange}
              required
            />
          </div>

          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-1">Deadline</label>
            <input
              name="deadline"
              type="date"
              className="w-full border rounded-lg px-3 py-2 focus:ring-2 focus:ring-indigo-400"
              value={formData.deadline}
              onChange={handleChange}
              required
            />
          </div>
        </div>

        <div>
          <label className="block text-sm font-semibold text-slate-700 mb-1">Attachments</label>
          <input
            type="file"
            multiple
            onChange={handleFileChange}
            className="w-full border rounded-lg px-3 py-2 text-sm text-slate-600"
          />
        </div>

        <button
          type="submit"
          disabled={loading}
          className="w-full bg-indigo-600 hover:bg-indigo-700 text-white py-2 rounded-lg font-semibold transition-all"
        >
          {loading ? "Submitting..." : "Submit RFQ"}
        </button>
      </form>
    </div>
  );
};

export default RfqForm;
