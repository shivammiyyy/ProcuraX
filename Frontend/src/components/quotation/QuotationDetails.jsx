import React, { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { getQuotationById, updateQuotation } from "../../api/quotationApi";
import { useAuth } from "../../context/AuthContext";

const QuotationDetails = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();

  const [quotation, setQuotation] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [updating, setUpdating] = useState(false);
  const [message, setMessage] = useState("");

  // Fetch quotation details
  useEffect(() => {
    const fetchQuotation = async () => {
      try {
        const response = await getQuotationById(id);
        const data = response.data.quotation || response.data;
        setQuotation(data);
      } catch (err) {
        setError(err.response?.data?.message || "Failed to load quotation details.");
      } finally {
        setLoading(false);
      }
    };
    fetchQuotation();
  }, [id]);

  // Update quotation status
  const handleUpdateStatus = async (newStatus) => {
    if (!isRfqOwner) {
      setError("You are not authorized to perform this action.");
      return;
    }

    setUpdating(true);
    setMessage("");
    try {
      await updateQuotation(id, { status: newStatus });
      setQuotation(prev => ({ ...prev, status: newStatus }));
      setMessage(`Quotation ${newStatus} successfully.`);

      if (newStatus === "accepted") {
        setTimeout(() => navigate(`/contracts/create/${id}`), 1200);
      }
    } catch (err) {
      setError(err.response?.data?.message || `Failed to ${newStatus} quotation.`);
    } finally {
      setUpdating(false);
    }
  };

  if (loading) return <p className="text-center py-6">Loading quotation details...</p>;
  if (error) return <p className="text-center text-red-600 py-6">{error}</p>;
  if (!quotation) return <p className="text-center py-6">No quotation found.</p>;

  // Role check
  const isRfqOwner =
    user.role === "buyer" &&
    (quotation.rfq?.Buyer === user._id || quotation.Buyer === user._id);
  const isVendor = user.role === "vendor" && quotation.vendor?._id === user._id;

  if (!isRfqOwner && !isVendor) {
    return <p className="text-center text-red-600 py-6">You are not authorized to view this quotation.</p>;
  }

  return (
    <div className="bg-white p-8 rounded shadow max-w-4xl mx-auto mt-10 border border-gray-200 space-y-6">
      <h2 className="text-2xl font-bold text-center mb-4">Quotation Details</h2>

      {message && <p className="text-green-600 text-center">{message}</p>}

      {/* RFQ Info */}
      <div className="space-y-2">
        <p><strong>RFQ Title:</strong> {quotation.rfq?.title || "Untitled"}</p>
        <p><strong>Description:</strong> {quotation.rfq?.description || "N/A"}</p>
        <p><strong>Budget:</strong> ₹{quotation.rfq?.budget?.toLocaleString() || "N/A"}</p>
        <p><strong>Status:</strong> <span className="capitalize">{quotation.status}</span></p>
      </div>

      {/* Vendor Info */}
      <div className="mt-4">
        <h3 className="font-semibold text-lg mb-2">Vendor</h3>
        <p><strong>Name:</strong> {quotation.vendor?.fullName}</p>
        <p><strong>Company:</strong> {quotation.vendor?.companyName}</p>
        <p><strong>Email:</strong> {quotation.vendor?.email || "N/A"}</p>
      </div>

      {/* Quotation Details */}
      <div className="mt-4">
        <h3 className="font-semibold text-lg mb-2">Quotation Details</h3>
        <p><strong>Quoted Price:</strong> ₹{quotation.price?.toLocaleString() || "N/A"}</p>
        <p><strong>Delivery Time:</strong> {quotation.deliveryTimeDays} days</p>
        <p><strong>Vendor Score:</strong> {quotation.vendorScore?.toFixed(2) || "N/A"}</p>
      </div>

      {/* Compliance Info */}
      {quotation.compliance?.length > 0 && (
        <div className="mt-4">
          <h3 className="font-semibold text-lg mb-2">Compliance</h3>
          {quotation.compliance.map((c, index) => (
            <div key={index} className="p-3 bg-gray-50 border rounded mb-2">
              <p><strong>ISO Certification:</strong> {c.ISO_Certification ? "Yes" : "No"}</p>
              <p><strong>Material Grade:</strong> {c.Material_Grade}</p>
              <p><strong>Environmental Standards:</strong> {c.Environmental_Standards ? "Yes" : "No"}</p>
              <p><strong>Document Submission:</strong> {c.Document_Submission ? "Yes" : "No"}</p>
            </div>
          ))}
        </div>
      )}

      {/* Attachments */}
      {quotation.attachments?.length > 0 && (
        <div className="mt-4">
          <h3 className="font-semibold text-lg mb-2">Attachments</h3>
          <ul className="space-y-2">
            {quotation.attachments.map(file => (
              <li key={file._id}>
                <a
                  href={file.filePath}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-blue-600 hover:underline mr-4"
                >
                  {file.fileName}
                </a>
                <a
                  href={file.filePath}
                  download
                  className="bg-gray-200 hover:bg-gray-300 text-gray-800 px-2 py-1 rounded"
                >
                  Download
                </a>
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* Actions */}
      <div className="mt-6 flex flex-wrap gap-4 justify-center">
        {isRfqOwner && quotation.status !== "accepted" && quotation.status !== "rejected" && (
          <>
            <button
              onClick={() => handleUpdateStatus("accepted")}
              disabled={updating}
              className="bg-green-600 hover:bg-green-700 text-white font-semibold py-2 px-4 rounded transition"
            >
              {updating ? "Updating..." : "Accept & Create Contract"}
            </button>
            <button
              onClick={() => handleUpdateStatus("rejected")}
              disabled={updating}
              className="bg-red-600 hover:bg-red-700 text-white font-semibold py-2 px-4 rounded transition"
            >
              {updating ? "Updating..." : "Reject"}
            </button>
          </>
        )}

        {isRfqOwner && quotation.status === "accepted" && (
          <button
            onClick={() => navigate(`/contracts/create/${quotation._id}`)}
            className="bg-blue-600 hover:bg-blue-700 text-white font-semibold py-2 px-4 rounded transition"
          >
            Create Contract
          </button>
        )}
      </div>
    </div>
  );
};

export default QuotationDetails;
