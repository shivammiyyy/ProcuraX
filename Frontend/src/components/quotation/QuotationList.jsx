import React from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

const QuotationList = ({ quotations }) => {
  const { user } = useAuth();

  // Filter quotations: buyers see all for their RFQs, vendors see only their own
  const filteredQuotations = quotations.filter((q) => {
    if (user.role === 'buyer') {
      return q.rfq?.Buyer?._id === user._id;
    }
    return q.vendor?._id === user._id;
  });

  return (
    <div className="space-y-4">
      {filteredQuotations.length > 0 ? (
        filteredQuotations.map((quotation) => (
          <div
            key={quotation._id}
            className="bg-white p-4 rounded shadow flex justify-between items-center"
          >
            <div>
              <h2 className="text-lg font-semibold">{quotation.rfq?.title || 'RFQ'}</h2>
              <p className="text-gray-600">
                Price: ₹{quotation.price?.toLocaleString() || 'N/A'} | Delivery:{' '}
                {quotation.deliveryTimeDays} days
              </p>
              <p className="text-gray-500 text-sm">
                Status: <span className="capitalize">{quotation.status || 'pending'}</span>
              </p>
            </div>
            <Link
              to={`/quotations/${quotation._id}`}
              className="text-blue-600 hover:underline font-semibold"
            >
              View Details
            </Link>
          </div>
        ))
      ) : (
        <p>No quotations available.</p>
      )}
    </div>
  );
};

export default QuotationList;