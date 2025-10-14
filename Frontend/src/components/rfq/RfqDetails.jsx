import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { getQuotationByRFQId } from "../../api/rfqApi";

const RfqDetails = ({ rfq }) => {
  const { user } = useAuth();
  const [quotations, setQuotations] = useState([]);
  const [loadingQuotations, setLoadingQuotations] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    if (!rfq?._id) return;

    const fetchQuotations = async () => {
      try {
        const response = await getQuotationByRFQId(rfq._id);
        const data = response?.data?.quotations || response?.quotations || [];
        setQuotations(data);
      } catch (err) {
        console.error("Failed to load quotations:", err);
      } finally {
        setLoadingQuotations(false);
      }
    };

    fetchQuotations();
  }, [rfq?._id]);

  if (!rfq) {
    return <div className="text-center text-gray-600 p-6">Loading RFQ details...</div>;
  }

  const isRfqOwner = user.role === "buyer" && rfq.Buyer?._id === user._id;

  return (
    <div className="bg-white p-8 rounded shadow-md max-w-4xl mx-auto mt-6">
      {/* RFQ Header */}
      <h2 className="text-3xl font-bold mb-2">{rfq.title || "Untitled RFQ"}</h2>
      <p className="text-gray-500 mb-6">
        Posted by <b>{rfq.Buyer?.companyName || rfq.Buyer?.fullName || "Unknown Buyer"}</b>
      </p>

      {/* RFQ Info */}
      <div className="mb-6 space-y-2">
        <p><strong>Description:</strong> {rfq.description}</p>
        <p><strong>Request Type:</strong> {rfq.requestType}</p>
        <p><strong>Category:</strong> {rfq.category}</p>
        <p><strong>Budget:</strong> ₹{rfq.budget?.toLocaleString() || "N/A"}</p>
        <p><strong>Deadline:</strong> {rfq.deadline ? new Date(rfq.deadline).toLocaleDateString() : "N/A"}</p>
        <p><strong>Status:</strong> <span className="capitalize">{rfq.status}</span></p>
      </div>

      {/* RFQ Attachments */}
      {rfq.attachments?.length > 0 && (
        <div className="mb-6">
          <h3 className="text-xl font-semibold mb-2">RFQ Attachments</h3>
          <ul className="list-disc ml-5 space-y-1">
            {rfq.attachments.map((file, idx) => (
              <li key={idx}>
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

      {/* Quotations Table */}
      <div className="border-t pt-6 mt-6">
        <h3 className="text-2xl font-semibold mb-4">Submitted Quotations</h3>

        {loadingQuotations ? (
          <p className="text-gray-600">Loading quotations...</p>
        ) : quotations.length === 0 ? (
          <p className="text-gray-600">No quotations submitted yet.</p>
        ) : (
          <table className="w-full border border-gray-200 rounded-lg">
            <thead className="bg-gray-100">
              <tr>
                <th className="text-left p-3 border-b">Vendor</th>
                <th className="text-left p-3 border-b">Price</th>
                <th className="text-left p-3 border-b">Delivery (days)</th>
                <th className="text-left p-3 border-b">Status</th>
                <th className="text-left p-3 border-b">Attachments</th>
                <th className="text-left p-3 border-b">Action</th>
              </tr>
            </thead>
            <tbody>
              {quotations.map((q) => (
                <tr key={q._id} className="border-b hover:bg-gray-50">
                  <td className="p-3">{q.vendor?.companyName || q.vendor?.fullName || "Unknown Vendor"}</td>
                  <td className="p-3">₹{q.price?.toLocaleString() || "N/A"}</td>
                  <td className="p-3">{q.deliveryTimeDays || "N/A"}</td>
                  <td className="p-3 capitalize">{q.status || "pending"}</td>

                  {/* Attachments */}
                  <td className="p-3">
                    {q.attachments?.length > 0 ? (
                      <ul className="space-y-1">
                        {q.attachments.map((file) => (
                          <li key={file._id}>
                            <a
                              href={file.filePath}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="text-blue-600 hover:underline mr-2"
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
                    ) : (
                      "—"
                    )}
                  </td>

                  {/* Action */}
                  <td className="p-3">
                    {(isRfqOwner || q.vendor?._id === user._id) && (
                      <button
                        onClick={() => navigate(`/quotations/${q._id}`)}
                        className="bg-blue-600 text-white px-3 py-1 rounded hover:bg-blue-700"
                      >
                        View
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
};

export default RfqDetails;
