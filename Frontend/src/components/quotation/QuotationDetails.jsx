import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getQuotationsForRfq } from '../../api/rfqApi';
import { useAuth } from '../../context/AuthContext';

const RfqDetails = ({ rfq }) => {
  const { user } = useAuth();
  const [quotations, setQuotations] = useState([]);
  const [loadingQuotations, setLoadingQuotations] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    if (!rfq?._id) return;

    const fetchQuotations = async () => {
      try {
        const response = await getQuotationsForRfq(rfq._id);
        // Filter quotations to ensure rfq._id matches quotation.rfqId or quotation.rfq._id
        const filteredQuotations = (response?.data?.quotations || []).filter(
          (q) => q.rfqId === rfq._id || q.rfq?._id === rfq._id
        );
        setQuotations(filteredQuotations);
      } catch (err) {
        console.error('Failed to load quotations:', err);
      } finally {
        setLoadingQuotations(false);
      }
    };

    fetchQuotations();
  }, [rfq?._id]);

  if (!rfq) {
    return <div className="text-center text-gray-600 p-6">Loading RFQ details...</div>;
  }

  const isRfqOwner = user.role === 'buyer' && rfq.Buyer?._id === user._id;

  return (
    <div className="bg-white p-8 rounded shadow-md max-w-3xl mx-auto">
      <h2 className="text-3xl font-bold mb-2">{rfq.title || 'Untitled RFQ'}</h2>
      <p className="text-gray-500 mb-6">
        Posted by <b>{rfq.Buyer?.companyName || rfq.Buyer?.fullName || 'Unknown Buyer'}</b>
      </p>

      {/* RFQ Info */}
      <div className="mb-6">
        <h3 className="text-xl font-semibold mb-2">Description</h3>
        <p className="text-gray-700 whitespace-pre-line">{rfq.description}</p>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-6">
        <div>
          <p className="font-semibold">Request Type:</p>
          <p>{rfq.requestType}</p>
        </div>
        <div>
          <p className="font-semibold">Category:</p>
          <p>{rfq.category}</p>
        </div>
        <div>
          <p className="font-semibold">Budget:</p>
          <p>₹{rfq.budget?.toLocaleString()}</p>
        </div>
        <div>
          <p className="font-semibold">Deadline:</p>
          <p>{rfq.deadline ? new Date(rfq.deadline).toLocaleDateString() : 'N/A'}</p>
        </div>
        <div>
          <p className="font-semibold">Status:</p>
          <p className="capitalize">{rfq.status}</p>
        </div>
      </div>

      {/* Attachments */}
      {rfq.attachments?.length > 0 && (
        <div className="mb-6">
          <h3 className="text-xl font-semibold mb-2">Attachments</h3>
          <ul className="list-disc ml-5 space-y-1">
            {rfq.attachments.map((file, index) => (
              <li key={index}>
                <a
                  href={file.filePath}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-blue-600 hover:underline"
                >
                  {file.fileName}
                </a>
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* Quotations */}
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
                <th className="text-left p-3 border-b">Amount</th>
                <th className="text-left p-3 border-b">Status</th>
                <th className="text-left p-3 border-b">Action</th>
              </tr>
            </thead>
            <tbody>
              {quotations.map((q) => (
                <tr key={q._id} className="border-b hover:bg-gray-50">
                  <td className="p-3">{q.vendor?.companyName || q.vendor?.fullName || 'Unknown Vendor'}</td>
                  <td className="p-3">₹{q.price?.toLocaleString() || 'N/A'}</td>
                  <td className="p-3 capitalize">{q.status || 'pending'}</td>
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