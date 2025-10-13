import React from 'react';

const FileUploader = ({ onFileSelect }) => {
  const handleFileChange = (e) => {
    if (e.target.files && e.target.files.length > 0) {
      onFileSelect(e.target.files[0]);
    }
  };

  return (
    <div className="file-uploader">
      <input type="file" onChange={handleFileChange} />
    </div>
  );
};

export default FileUploader;
