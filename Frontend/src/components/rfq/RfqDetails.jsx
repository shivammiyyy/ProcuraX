import React from 'react';

const RfqDetails = ({ rfq }) => {
  if (!rfq) {
    return (
      <div className="text-center text-gray-600 p-6">
        Loading RFQ details...
      </div>
    );
  }

  return (
    <div className="bg-white p-8 rounded shadow-md max-w-3xl mx-auto">
      {/* Title */}
      <h2 className="text-3xl font-bold mb-2">{rfq.title || 'Untitled RFQ'}</h2>
      <p className="text-gray-500 mb-6">
        Posted by <b>{rfq.Buyer?.companyName || rfq.Buyer?.fullName || 'Unknown Buyer'}</b>
      </p>

      {/* Description */}
      <div className="mb-6">
        <h3 className="text-xl font-semibold mb-2">Description</h3>
        <p className="text-gray-700 whitespace-pre-line">
          {rfq.description || 'No description provided.'}
        </p>
      </div>

      {/* RFQ Info */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-6">
        <div>
          <p className="font-semibold text-gray-800">Request Type:</p>
          <p className="text-gray-700 capitalize">{rfq.requestType || 'N/A'}</p>
        </div>
        <div>
          <p className="font-semibold text-gray-800">Category:</p>
          <p className="text-gray-700">{rfq.category || 'N/A'}</p>
        </div>
        <div>
          <p className="font-semibold text-gray-800">Budget:</p>
          <p className="text-gray-700">₹{rfq.budget || 'N/A'}</p>
        </div>
        <div>
          <p className="font-semibold text-gray-800">Deadline:</p>
          <p className="text-gray-700">
            {rfq.deadline ? new Date(rfq.deadline).toLocaleDateString() : 'N/A'}
          </p>
        </div>
        <div>
          <p className="font-semibold text-gray-800">Status:</p>
          <p className="text-gray-700 capitalize">{rfq.status || 'open'}</p>
        </div>
      </div>

      {/* Attachments */}
      {rfq.attachments && rfq.attachments.length > 0 && (
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

      {/* Buyer Info */}
      {rfq.Buyer && (
        <div className="border-t pt-4 mt-6 text-gray-700">
          <h3 className="text-lg font-semibold mb-2">Buyer Information</h3>
          <p>
            <span className="font-semibold">Name:</span>{' '}
            {rfq.Buyer.fullName || 'N/A'}
          </p>
          <p>
            <span className="font-semibold">Company:</span>{' '}
            {rfq.Buyer.companyName || 'N/A'}
          </p>
          <p>
            <span className="font-semibold">Email:</span>{' '}
            {rfq.Buyer.email || 'N/A'}
          </p>
        </div>
      )}
    </div>
  );
};

export default RfqDetails;
