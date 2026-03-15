
import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { ServiceCard } from '../types';
import { getIcon } from '../constants';
import { ArrowRight, Sparkles } from 'lucide-react';
import { useTheme } from '../theme';

interface ProactiveServiceKitProps {
  cards: ServiceCard[];
  onCardClick?: (card: ServiceCard) => void;
  headerTip?: string;
}

const ProactiveServiceKit: React.FC<ProactiveServiceKitProps> = ({ cards, onCardClick, headerTip }) => {
  const { theme } = useTheme();
  return (
    <div className="flex flex-col gap-6 w-full">
      <AnimatePresence mode="wait">
        {headerTip && (
          <motion.div
            initial={{ opacity: 0, x: -10 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: 10 }}
            className="flex items-center gap-3 mb-2"
          >
            <div className={`w-1.5 h-6 rounded-full bg-gradient-to-b ${theme.colors.robotAuraOuter}`} />
            <span className={`text-base font-black tracking-widest uppercase ${theme.colors.textMuted} transition-colors duration-500`}>
              {headerTip}
            </span>
          </motion.div>
        )}
      </AnimatePresence>

      <AnimatePresence mode="popLayout">
        {cards.map((card) => {
          return (
            <motion.div
              key={card.id}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -20 }}
              whileHover={{ 
                y: -4,
                scale: 1.02,
              }}
              onClick={() => onCardClick?.(card)}
              className={`
                group w-full p-6 rounded-[32px] flex items-center gap-5 cursor-pointer relative overflow-hidden
                transition-all active:scale-95 duration-500
                ${theme.colors.cardBg} shadow-[0_20px_40px_-15px_rgba(0,0,0,0.05)] border ${theme.colors.cardBorder}
              `}
            >
              <div className={`
                w-16 h-16 rounded-[24px] flex items-center justify-center text-white 
                bg-gradient-to-tr ${theme.colors.robotAuraOuter}
                shadow-[0_8px_16px_-6px_rgba(249,115,22,0.4)]
                group-hover:scale-105 transition-transform
              `}>
                {getIcon(card.icon)}
              </div>
              
              <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2 mb-1">
                   <h3 className={`text-xl font-bold ${theme.colors.textPrimary} tracking-tight transition-colors duration-500`}>{card.title}</h3>
                   <Sparkles className={`w-5 h-5 ${theme.colors.accent} opacity-0 group-hover:opacity-100 transition-opacity animate-pulse`} />
                </div>
                <p className={`text-sm ${theme.colors.textMuted} font-medium leading-relaxed truncate transition-colors duration-500`}>{card.content}</p>
              </div>

              <div className={`w-12 h-12 rounded-full ${theme.colors.backgroundShapes} flex items-center justify-center group-hover:${theme.colors.accentBg}/10 transition-colors duration-500`}>
                <ArrowRight className={`w-5 h-5 ${theme.colors.textMuted} group-hover:${theme.colors.accent} transition-colors`} />
              </div>

              {/* Progress timer bar at the bottom of the card */}
              <div className={`absolute bottom-0 left-[12.5%] h-1.5 ${theme.colors.backgroundShapes} w-3/4 overflow-hidden rounded-t-full transition-colors duration-500`}>
                <motion.div 
                   key={card.id + "-timer"}
                   initial={{ width: 0 }}
                   animate={{ width: "100%" }}
                   transition={{ duration: 10, ease: "linear" }}
                   className={`h-full ${theme.colors.accentBg} rounded-full`}
                />
              </div>
            </motion.div>
          );
        })}
      </AnimatePresence>
    </div>
  );
};

export default ProactiveServiceKit;
