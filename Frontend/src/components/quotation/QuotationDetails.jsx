import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { getQuotationById, updateQuotation } from "../../api/quotationApi";
import { useAuth } from "../../context/AuthContext";
import { motion } from "framer-motion";
import { CheckCircle, XCircle, FileText, Loader2 } from "lucide-react";

const QuotationDetails = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();

  const [quotation, setQuotation] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [updating, setUpdating] = useState(false);
  const [message, setMessage] = useState("");

  useEffect(() => {
    const fetchQuotation = async () => {
      try {
        const response = await getQuotationById(id);
        setQuotation(response.data.quotation || response.data);
      } catch (err) {
        setError(err.response?.data?.message || "Failed to load quotation details.");
      } finally {
        setLoading(false);
      }
    };
    fetchQuotation();
  }, [id]);

  const isRfqOwner =
    user.role === "buyer" &&
    (quotation?.rfq?.Buyer === user._id || quotation?.Buyer === user._id);
  const isVendor = user.role === "vendor" && quotation?.vendor?._id === user._id;

  const handleUpdateStatus = async (newStatus) => {
    if (!isRfqOwner) {
      setError("You are not authorized to perform this action.");
      return;
    }
    setUpdating(true);
    setMessage("");
    try {
      await updateQuotation(id, { status: newStatus });
      setQuotation((prev) => ({ ...prev, status: newStatus }));
      setMessage(`Quotation ${newStatus} successfully.`);
      if (newStatus === "accepted") {
        setTimeout(() => navigate(`/contracts/create/${id}`), 1500);
      }
    } catch (err) {
      setError(err.response?.data?.message || `Failed to ${newStatus} quotation.`);
    } finally {
      setUpdating(false);
    }
  };

  if (loading)
    return (
      <div className="flex justify-center items-center py-20">
        <Loader2 className="animate-spin text-blue-600 w-8 h-8" />
      </div>
    );
  if (error)
    return <p className="text-center text-red-600 py-10 text-lg font-medium">{error}</p>;

  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="max-w-4xl mx-auto bg-white shadow-lg border border-gray-200 rounded-2xl p-8 mt-10 space-y-8"
    >
      <div className="border-b pb-4">
        <h2 className="text-3xl font-bold text-gray-800 flex items-center gap-2">
          <FileText className="text-blue-600" />
          Quotation Overview
        </h2>
        <p className="text-gray-500 text-sm mt-1">
          Review quotation details and take actions accordingly.
        </p>
      </div>

      {message && (
        <motion.p
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          className="bg-green-50 border border-green-300 text-green-700 p-3 rounded-md text-center"
        >
          {message}
        </motion.p>
      )}

      <div className="grid md:grid-cols-2 gap-6">
        <div className="bg-gray-50 p-4 rounded-lg border">
          <h3 className="font-semibold text-lg mb-3 text-gray-700">RFQ Information</h3>
          <p><strong>Title:</strong> {quotation.rfq?.title}</p>
          <p><strong>Description:</strong> {quotation.rfq?.description}</p>
          <p><strong>Budget:</strong> ₹{quotation.rfq?.budget?.toLocaleString()}</p>
        </div>

        <div className="bg-gray-50 p-4 rounded-lg border">
          <h3 className="font-semibold text-lg mb-3 text-gray-700">Vendor</h3>
          <p><strong>Name:</strong> {quotation.vendor?.fullName}</p>
          <p><strong>Company:</strong> {quotation.vendor?.companyName}</p>
          <p><strong>Email:</strong> {quotation.vendor?.email}</p>
        </div>
      </div>

      <div className="bg-gray-50 p-4 rounded-lg border">
        <h3 className="font-semibold text-lg mb-3 text-gray-700">Quotation Details</h3>
        <div className="flex justify-between items-center mb-2">
          <p><strong>Quoted Price:</strong> ₹{quotation.price?.toLocaleString()}</p>
          <p>
            <strong>Status:</strong>{" "}
            <span
              className={`px-2 py-1 rounded text-sm font-medium ${
                quotation.status === "accepted"
                  ? "bg-green-100 text-green-700"
                  : quotation.status === "rejected"
                  ? "bg-red-100 text-red-700"
                  : "bg-yellow-100 text-yellow-700"
              }`}
            >
              {quotation.status}
            </span>
          </p>
        </div>
        <p><strong>Delivery Time:</strong> {quotation.deliveryTimeDays} days</p>
        <p><strong>Vendor Score:</strong> {quotation.vendorScore?.toFixed(2)}</p>
      </div>

      {quotation.compliance?.length > 0 && (
        <div className="bg-gray-50 p-4 rounded-lg border">
          <h3 className="font-semibold text-lg mb-3 text-gray-700">Compliance</h3>
          {quotation.compliance.map((c, i) => (
            <div key={i} className="border-b last:border-none pb-2 mb-2">
              <p><strong>Material Grade:</strong> {c.Material_Grade}</p>
              <p><strong>ISO Certification:</strong> {c.ISO_Certification ? "Yes" : "No"}</p>
              <p><strong>Environmental Standards:</strong> {c.Environmental_Standards ? "Yes" : "No"}</p>
              <p><strong>Document Submission:</strong> {c.Document_Submission ? "Yes" : "No"}</p>
            </div>
          ))}
        </div>
      )}

      {quotation.attachments?.length > 0 && (
        <div className="bg-gray-50 p-4 rounded-lg border">
          <h3 className="font-semibold text-lg mb-3 text-gray-700">Attachments</h3>
          <ul className="space-y-2">
            {quotation.attachments.map((file) => (
              <li key={file._id} className="flex justify-between items-center">
                <a
                  href={file.filePath}
                  target="_blank"
                  className="text-blue-600 hover:underline font-medium"
                >
                  {file.fileName}
                </a>
                <a
                  href={file.filePath}
                  download
                  className="bg-blue-50 hover:bg-blue-100 text-blue-700 px-3 py-1 rounded text-sm"
                >
                  Download
                </a>
              </li>
            ))}
          </ul>
        </div>
      )}

      {isRfqOwner && (
        <div className="sticky bottom-0 bg-white pt-4 flex justify-center gap-4 border-t">
          <button
            onClick={() => handleUpdateStatus("accepted")}
            disabled={updating}
            className="bg-green-600 hover:bg-green-700 text-white px-6 py-2 rounded-lg font-medium flex items-center gap-2 transition"
          >
            {updating ? <Loader2 className="animate-spin w-4 h-4" /> : <CheckCircle size={18} />}
            Accept & Create Contract
          </button>
          <button
            onClick={() => handleUpdateStatus("rejected")}
            disabled={updating}
            className="bg-red-600 hover:bg-red-700 text-white px-6 py-2 rounded-lg font-medium flex items-center gap-2 transition"
          >
            <XCircle size={18} /> Reject
          </button>
        </div>
      )}
    </motion.div>
  );
};

export default QuotationDetails;
