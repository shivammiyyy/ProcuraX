import React from 'react';
import { Link } from 'react-router-dom';

const RfqList = ({ rfqs }) => {
  return (
    <div className="space-y-4">
      {rfqs.map((rfq) => (
        <div
          key={rfq._id}
          className="bg-white p-4 rounded shadow flex justify-between items-start"
        >
          <div className="flex-1 pr-4">
            <h2 className="text-xl font-semibold mb-1">{rfq.title || 'Untitled RFQ'}</h2>
            <p className="text-gray-600 mb-2">
              {rfq.description ? rfq.description.substring(0, 120) + '...' : 'No description'}
            </p>
            <p className="text-sm text-gray-500">
              <span className="font-semibold">Category:</span> {rfq.category || 'N/A'} |{' '}
              <span className="font-semibold">Budget:</span> ₹{rfq.budget || 'N/A'}
            </p>
            <p className="text-sm text-gray-500">
              <span className="font-semibold">Deadline:</span>{' '}
              {rfq.deadline ? new Date(rfq.deadline).toLocaleDateString() : 'N/A'} |{' '}
              <span className="font-semibold">Status:</span>{' '}
              <span className="capitalize">{rfq.status || 'open'}</span>
            </p>
          </div>
          <Link
            to={`/rfqs/${rfq._id}`}
            className="text-blue-600 hover:underline font-semibold mt-2"
          >
            View Details
          </Link>
        </div>
      ))}
      {rfqs.length === 0 && <p className="text-center text-gray-600">No RFQs available.</p>}
    </div>
  );
};

export default RfqList;
