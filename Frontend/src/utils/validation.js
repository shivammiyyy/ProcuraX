// Validation helpers for forms

export const isEmailValid = (email) => {
  const regex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return regex.test(email);
};

export const isPasswordValid = (password) => {
  // Minimum 8 characters, with at least one letter and one number
  const regex = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{8,}$/;
  return regex.test(password);
};

export const isNotEmpty = (value) => {
  return value && value.trim() !== '';
};

export const validateRfQForm = (data) => {
  const errors = {};
  if (!isNotEmpty(data.title)) errors.title = 'Title is required';
  if (!isNotEmpty(data.description)) errors.description = 'Description is required';
  if (!isNotEmpty(data.deadline)) errors.deadline = 'Deadline is required';
  // Add more validation as needed
  return errors;
};
