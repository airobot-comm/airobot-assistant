export type ThemeMode = 'light' | 'dark';

export interface ThemeConfig {
  colors: {
    background: string;
    backgroundShapes: string;
    textPrimary: string;
    textSecondary: string;
    textMuted: string;
    accent: string;
    accentHover: string;
    accentBg: string;
    cardBg: string;
    cardBorder: string;
    bubbleBg: string;
    bubbleBorder: string;
    robotAuraOuter: string;
    robotAuraInner: string;
    robotHead: string;
    robotFace: string;
    robotEyeDefault: string;
    robotEyeActive: string;
    robotEyeFocus: string;
    robotAntennaDefault: string;
    robotAntennaFocus: string;
    timerBg: string;
    timerRingBg: string;
    timerRingActive: string;
    timerRingPaused: string;
  };
  sizes: {
    robotWidth: string;
    robotHeight: string;
    cardWidth: string;
    cardHeight: string;
    sidebarWidth: string;
  };
}

export const lightTheme: ThemeConfig = {
  colors: {
    background: 'bg-slate-200',
    backgroundShapes: 'bg-slate-300/40',
    textPrimary: 'text-slate-700',
    textSecondary: 'text-slate-600',
    textMuted: 'text-slate-400',
    accent: 'text-orange-500',
    accentHover: 'text-orange-600',
    accentBg: 'bg-orange-500',
    cardBg: 'bg-white/90',
    cardBorder: 'border-slate-200',
    bubbleBg: 'bg-white/40',
    bubbleBorder: 'border-white/50',
    robotAuraOuter: 'from-sky-400/60 to-blue-300/60',
    robotAuraInner: 'border-sky-300/40',
    robotHead: 'bg-sky-200', // Distinct Light Blue
    robotFace: 'bg-sky-100', // Softer Light Blue
    robotEyeDefault: 'bg-slate-800',
    robotEyeActive: 'bg-orange-500',
    robotEyeFocus: 'bg-emerald-500',
    robotAntennaDefault: 'bg-orange-500', // Unified Orange
    robotAntennaFocus: 'bg-emerald-400',
    timerBg: 'bg-white',
    timerRingBg: 'stroke-slate-100',
    timerRingActive: 'stroke-orange-400',
    timerRingPaused: 'stroke-slate-300',
  },
  sizes: {
    robotWidth: 'w-[660px]',
    robotHeight: 'h-[500px]',
    cardWidth: 'w-[420px]',
    cardHeight: 'h-[620px]',
    sidebarWidth: 'w-[300px]',
  }
};

export const darkTheme: ThemeConfig = {
  colors: {
    background: 'bg-slate-900',
    backgroundShapes: 'bg-slate-800/40',
    textPrimary: 'text-slate-100',
    textSecondary: 'text-slate-300',
    textMuted: 'text-slate-500',
    accent: 'text-indigo-400',
    accentHover: 'text-indigo-300',
    accentBg: 'bg-indigo-500',
    cardBg: 'bg-slate-800/90',
    cardBorder: 'border-slate-700',
    bubbleBg: 'bg-slate-800/40',
    bubbleBorder: 'border-slate-700/50',
    robotAuraOuter: 'from-sky-500/40 to-blue-500/40',
    robotAuraInner: 'border-sky-400/30',
    robotHead: 'bg-sky-200', // Distinct Light Blue
    robotFace: 'bg-sky-100', // Softer Light Blue
    robotEyeDefault: 'bg-slate-800', // Unified Dark Eyes
    robotEyeActive: 'bg-orange-500', // Unified Orange
    robotEyeFocus: 'bg-emerald-400',
    robotAntennaDefault: 'bg-orange-500', // Unified Orange
    robotAntennaFocus: 'bg-emerald-400',
    timerBg: 'bg-slate-800',
    timerRingBg: 'stroke-slate-700',
    timerRingActive: 'stroke-indigo-400',
    timerRingPaused: 'stroke-slate-600',
  },
  sizes: {
    robotWidth: 'w-[660px]',
    robotHeight: 'h-[500px]',
    cardWidth: 'w-[420px]',
    cardHeight: 'h-[620px]',
    sidebarWidth: 'w-[300px]',
  }
};

import React, { createContext, useContext, useState } from 'react';

const ThemeContext = createContext<{ theme: ThemeConfig; toggleTheme: () => void; mode: ThemeMode }>({
  theme: lightTheme,
  toggleTheme: () => {},
  mode: 'light'
});

export const ThemeProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [mode, setMode] = useState<ThemeMode>('light');
  
  const toggleTheme = () => {
    setMode(prev => prev === 'light' ? 'dark' : 'light');
  };

  const theme = mode === 'light' ? lightTheme : darkTheme;

  return (
    <ThemeContext.Provider value={{ theme, toggleTheme, mode }}>
      {children}
    </ThemeContext.Provider>
  );
};

export const useTheme = () => useContext(ThemeContext);
