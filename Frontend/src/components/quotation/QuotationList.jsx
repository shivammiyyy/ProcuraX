import React from 'react';
import { Link } from 'react-router-dom';

const QuotationList = ({ quotations }) => {
  return (
    <div className="space-y-4">
      {quotations.length > 0 ? (
        quotations.map((quotation) => (
          <div
            key={quotation._id}
            className="bg-white p-4 rounded shadow flex justify-between items-center"
          >
            <div>
              <h2 className="text-lg font-semibold">
                {quotation.rfq?.title || 'RFQ'}
              </h2>
              <p className="text-gray-600">
                Price: ₹{quotation.price} | Delivery: {quotation.deliveryTimeDays} days
              </p>
              <p className="text-gray-500 text-sm">
                Status: {quotation.status || 'pending'}
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
