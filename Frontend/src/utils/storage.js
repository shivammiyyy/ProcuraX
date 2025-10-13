// Utility to handle localStorage with JSON parse/stringify

export const setItem = (key, value) => {
  localStorage.setItem(key, JSON.stringify(value));
};

export const getItem = (key) => {
  const value = localStorage.getItem(key);
  try {
    return value ? JSON.parse(value) : null;
  } catch (e) {
    console.error('Error parsing JSON from localStorage', e);
    return null;
  }
};

export const removeItem = (key) => {
  localStorage.removeItem(key);
};
