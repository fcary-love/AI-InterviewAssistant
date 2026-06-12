/** @type {import('tailwindcss').Config} */
export default {
  content: [
    './index.html',
    './src/**/*.{vue,js,ts,jsx,tsx}'
  ],
  theme: {
    extend: {
      colors: {
        aurora: {
          bg: '#09090b',
          surface: '#18181b',
          border: '#27272a',
          green: '#10b981',
          'green-dim': 'rgba(16,185,129,0.10)',
          'green-glow': 'rgba(16,185,129,0.08)',
        }
      },
      fontFamily: {
        display: ['"DM Serif Display"', '"Noto Serif SC"', 'serif'],
        body: ['"Outfit"', '"Noto Sans SC"', '"PingFang SC"', '"Microsoft YaHei"', 'sans-serif'],
        mono: ['"JetBrains Mono"', '"Fira Code"', 'monospace'],
      },
      boxShadow: {
        'aurora': '0 0 40px rgba(16,185,129,0.08)',
        'aurora-lg': '0 0 60px rgba(16,185,129,0.12)',
        'card': '0 2px 8px rgba(0,0,0,0.3), 0 8px 24px rgba(0,0,0,0.18)',
        'elevated': '0 8px 32px rgba(0,0,0,0.45), 0 16px 64px rgba(0,0,0,0.25)',
      },
      borderRadius: {
        'xl': '18px',
        '2xl': '24px',
        '3xl': '28px',
      },
      keyframes: {
        'fade-in-up': {
          from: { opacity: '0', transform: 'translateY(16px)' },
          to: { opacity: '1', transform: 'translateY(0)' },
        },
        'fade-in': {
          from: { opacity: '0' },
          to: { opacity: '1' },
        },
        'scale-in': {
          from: { opacity: '0', transform: 'scale(0.95)' },
          to: { opacity: '1', transform: 'scale(1)' },
        },
        'glow-breathe': {
          '0%, 100%': { opacity: '0.4', filter: 'blur(20px)' },
          '50%': { opacity: '0.65', filter: 'blur(28px)' },
        },
      },
      animation: {
        'fade-in-up': 'fade-in-up 0.6s cubic-bezier(0.16,1,0.3,1) both',
        'fade-in': 'fade-in 0.4s cubic-bezier(0.16,1,0.3,1) both',
        'scale-in': 'scale-in 0.4s cubic-bezier(0.16,1,0.3,1) both',
        'glow-breathe': 'glow-breathe 3s ease-in-out infinite',
      },
    },
  },
  plugins: [],
}
