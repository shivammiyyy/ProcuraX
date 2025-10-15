import React, { useEffect, useState } from "react";
import { getQuotations } from "../../api/quotationApi";
import { useAuth } from "../../context/AuthContext";
import { motion } from "framer-motion";
import { FileText, Loader2, Search } from "lucide-react";
import { Link } from "react-router-dom";

const QuotationList = () => {
  const { user } = useAuth();
  const [quotations, setQuotations] = useState([]);
  const [filtered, setFiltered] = useState([]);
  const [loading, setLoading] = useState(true);
  const [query, setQuery] = useState("");

  useEffect(() => {
    const fetchData = async () => {
      try {
        const res = await getQuotations();
        setQuotations(res.data);
        setFiltered(res.data);
      } catch (err) {
        console.error(err);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  const handleSearch = (e) => {
    const value = e.target.value.toLowerCase();
    setQuery(value);
    const result = quotations.filter(
      (q) =>
        q.rfq?.title.toLowerCase().includes(value) ||
        q.vendor?.fullName.toLowerCase().includes(value)
    );
    setFiltered(result);
  };

  if (loading)
    return (
      <div className="flex justify-center items-center h-96">
        <Loader2 className="animate-spin text-blue-600 w-8 h-8" />
      </div>
    );

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-50 via-white to-blue-50 py-10 px-6">
      <div className="max-w-6xl mx-auto space-y-8">
        {/* Header */}
        <div className="flex flex-col sm:flex-row justify-between items-center gap-4">
          <div>
            <h2 className="text-3xl font-bold text-gray-800 flex items-center gap-2">
              <FileText className="text-blue-600" />
              Quotations
            </h2>
            <p className="text-gray-500 text-sm">
              View and manage all quotations in your procurement system.
            </p>
          </div>
          <div className="relative w-full sm:w-72">
            <Search className="absolute left-3 top-2.5 text-gray-400 w-4 h-4" />
            <input
              type="text"
              value={query}
              onChange={handleSearch}
              placeholder="Search by RFQ or Vendor"
              className="w-full pl-9 pr-3 py-2 border rounded-lg shadow-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
            />
          </div>
        </div>

        {/* Quotation Cards */}
        {filtered.length === 0 ? (
          <p className="text-gray-600 text-center py-20">
            No quotations found.
          </p>
        ) : (
          <motion.div
            layout
            className="grid md:grid-cols-2 lg:grid-cols-3 gap-6"
          >
            {filtered.map((quotation, index) => (
              <motion.div
                key={quotation._id}
                initial={{ opacity: 0, y: 15 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: index * 0.05 }}
                className="bg-white border border-gray-200 rounded-2xl shadow hover:shadow-lg transition-all p-5 flex flex-col justify-between"
              >
                <div>
                  <h3 className="font-semibold text-lg text-gray-800 mb-2">
                    {quotation.rfq?.title}
                  </h3>
                  <p className="text-gray-500 text-sm mb-3">
                    Vendor: {quotation.vendor?.fullName || "N/A"}
                  </p>
                  <div className="flex justify-between items-center mb-2">
                    <span className="font-semibold text-gray-700">
                      ₹{quotation.price?.toLocaleString()}
                    </span>
                    <span
                      className={`px-2 py-1 rounded text-xs font-medium ${
                        quotation.status === "accepted"
                          ? "bg-green-100 text-green-700"
                          : quotation.status === "rejected"
                          ? "bg-red-100 text-red-700"
                          : "bg-yellow-100 text-yellow-700"
                      }`}
                    >
                      {quotation.status}
                    </span>
                  </div>
                </div>
                <Link
                  to={`/quotations/${quotation._id}`}
                  className="mt-3 inline-block bg-blue-600 hover:bg-blue-700 text-white text-sm font-medium py-2 rounded-lg text-center transition"
                >
                  View Details
                </Link>
              </motion.div>
            ))}
          </motion.div>
        )}
      </div>
    </div>
  );
};

export default QuotationList;
