/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        comabel: {
          blue: '#004B9B',
          'blue-dark': '#003366',
          'blue-light': '#EBF3FA',
          red: '#E30613',
          'red-dark': '#B8000A',
          'red-light': '#FDE8E9',
        }
      }
    },
  },
  plugins: [],
}