import React from 'react';

const QuotationDetails = ({ quotation }) => {
  if (!quotation) return <p>Loading...</p>;

  const {
    rfq,
    vendor,
    price,
    deliveryTimeDays,
    compliance,
    vendorScore,
    attachments,
    status,
  } = quotation;

  return (
    <div className="bg-white p-8 rounded shadow-md max-w-3xl mx-auto">
      <h2 className="text-3xl font-bold mb-4">
        Quotation for {rfq?.title || 'RFQ'}
      </h2>

      <div className="mb-4 text-gray-700">
        <p className="mb-2">
          <span className="font-semibold">RFQ Description:</span>{' '}
          {rfq?.description || 'No description provided.'}
        </p>
        <p className="mb-2">
          <span className="font-semibold">Vendor:</span>{' '}
          {vendor?.companyName || vendor?.fullName}
        </p>
        <p className="mb-2">
          <span className="font-semibold">Price:</span> ₹{price}
        </p>
        <p className="mb-2">
          <span className="font-semibold">Delivery Time:</span>{' '}
          {deliveryTimeDays} days
        </p>
        <p className="mb-2">
          <span className="font-semibold">Vendor Score:</span>{' '}
          {vendorScore?.toFixed(2) || 'N/A'}
        </p>
        <p className="mb-4">
          <span className="font-semibold">Status:</span>{' '}
          <span className="capitalize">{status || 'pending'}</span>
        </p>
      </div>

      <h3 className="text-xl font-semibold mb-2">Compliance Details</h3>
      <ul className="list-disc list-inside text-gray-600 mb-4">
        <li>ISO Certification: {compliance?.ISO_Certification ? 'Yes' : 'No'}</li>
        <li>Material Grade: {compliance?.Material_Grade || 'N/A'}</li>
        <li>Environmental Standards: {compliance?.Environmental_Standards ? 'Yes' : 'No'}</li>
        <li>Document Submission: {compliance?.Document_Submission ? 'Yes' : 'No'}</li>
      </ul>

      {attachments?.length > 0 && (
        <div>
          <h3 className="text-xl font-semibold mb-2">Attachments</h3>
          {attachments.map((file, idx) => (
            <a
              key={idx}
              href={file.filePath}
              target="_blank"
              rel="noopener noreferrer"
              className="block text-blue-600 hover:underline mb-1"
            >
              {file.fileName}
            </a>
          ))}
        </div>
      )}
    </div>
  );
};

export default QuotationDetails;
