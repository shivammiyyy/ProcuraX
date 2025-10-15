import React, { useState, useEffect } from "react";
import { createQuotation } from "../../api/quotationApi";
import { getRfqList } from "../../api/rfqApi";
import { useAuth } from "../../context/AuthContext";
import { useNavigate } from "react-router-dom";
import { motion } from "framer-motion";
import { FilePlus, Loader2 } from "lucide-react";

const QuotationForm = () => {
  const { user } = useAuth();
  const [rfqs, setRfqs] = useState([]);
  const [formData, setFormData] = useState({
    rfq: "",
    price: "",
    deliveryTimeDays: "",
    attachments: [],
  });
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchRfqs = async () => {
      const res = await getRfqList();
      setRfqs(res.data);
    };
    fetchRfqs();
  }, []);

  const handleChange = (e) =>
    setFormData({ ...formData, [e.target.name]: e.target.value });

  const handleFileChange = (e) =>
    setFormData({ ...formData, attachments: [...e.target.files] });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const data = new FormData();
      data.append("rfq", formData.rfq);
      data.append("price", formData.price);
      data.append("deliveryTimeDays", formData.deliveryTimeDays);
      formData.attachments.forEach((file) => data.append("attachments", file));

      await createQuotation(data);
      navigate("/quotations");
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-50 via-white to-blue-50 flex justify-center py-10">
      <motion.form
        onSubmit={handleSubmit}
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="w-full max-w-2xl bg-white border border-gray-200 rounded-2xl shadow-lg p-8 space-y-6"
      >
        <div className="flex items-center gap-2 mb-2">
          <FilePlus className="text-blue-600" />
          <h2 className="text-2xl font-bold text-gray-800">Create Quotation</h2>
        </div>
        <p className="text-gray-500 text-sm">
          Submit a quotation for a Request for Quotation (RFQ) below.
        </p>

        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Select RFQ
            </label>
            <select
              name="rfq"
              value={formData.rfq}
              onChange={handleChange}
              required
              className="w-full border rounded-lg px-3 py-2 focus:ring-2 focus:ring-blue-500"
            >
              <option value="">-- Select RFQ --</option>
              {rfqs.map((r) => (
                <option key={r._id} value={r._id}>
                  {r.title}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Price (₹)
            </label>
            <input
              type="number"
              name="price"
              value={formData.price}
              onChange={handleChange}
              required
              className="w-full border rounded-lg px-3 py-2 focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Delivery Time (Days)
            </label>
            <input
              type="number"
              name="deliveryTimeDays"
              value={formData.deliveryTimeDays}
              onChange={handleChange}
              required
              className="w-full border rounded-lg px-3 py-2 focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">
              Attach Documents
            </label>
            <input
              type="file"
              name="attachments"
              multiple
              onChange={handleFileChange}
              className="w-full border rounded-lg px-3 py-2 focus:ring-2 focus:ring-blue-500"
            />
          </div>
        </div>

        <button
          type="submit"
          disabled={loading}
          className="w-full bg-blue-600 hover:bg-blue-700 text-white font-semibold py-2 rounded-lg flex items-center justify-center gap-2 transition"
        >
          {loading ? (
            <Loader2 className="animate-spin w-5 h-5" />
          ) : (
            "Submit Quotation"
          )}
        </button>
      </motion.form>
    </div>
  );
};

export default QuotationForm;
