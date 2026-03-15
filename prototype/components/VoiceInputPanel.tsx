
import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Mic, Command, MessageSquareText, Headphones, Hourglass, Pause, Square, Play } from 'lucide-react';
import { RobotState, ServiceCard } from '../types';
import { useTheme } from '../theme';

interface VoiceInputPanelProps {
  robotState: RobotState;
  onStartInteraction: () => void;
  onSimulatedSpeechEnd: (text: string) => void;
  activeCard?: ServiceCard | null;
  // Controls for timer in Focus mode
  timerStatus?: 'IDLE' | 'RUNNING' | 'PAUSED';
  onTimerControl?: (action: 'PAUSE' | 'RESUME' | 'STOP') => void;
  mini?: boolean;
  variant?: 'default' | 'mini' | 'card';
  dynamicSuggestions?: string[];
  suggestionError?: boolean;
}

const VoiceInputPanel: React.FC<VoiceInputPanelProps> = ({ 
  robotState, 
  onStartInteraction, 
  onSimulatedSpeechEnd,
  activeCard,
  timerStatus,
  onTimerControl,
  mini = false,
  variant = 'default',
  dynamicSuggestions,
  suggestionError = false
}) => {
  const { theme } = useTheme();
  
  // Normalize variant
  const activeVariant = mini ? 'mini' : variant;
  
  const isListening = robotState === 'LISTENING';
  const isThinking = robotState === 'THINKING';
  const isSpeaking = robotState === 'SPEAKING';
  
  // Active Timer Override
  const isTimerActive = timerStatus && timerStatus !== 'IDLE';

  // Determine suggestions based on active card
  const suggestions = React.useMemo(() => {
    if (suggestionError) {
        return ["暂无推荐 (AI未响应)"];
    }

    if (isTimerActive) {
      if (timerStatus === 'RUNNING') return ["暂停计时", "继续专注", "结束专注"];
      if (timerStatus === 'PAUSED') return ["继续计时", "重新开始", "结束专注"];
      return ["开始25分钟专注", "开始10分钟休息", "设置专注目标"];
    }

    if (dynamicSuggestions && dynamicSuggestions.length > 0) {
      return dynamicSuggestions.slice(0, 3);
    }

    // If card mode, return empty to avoid fallback (as requested by user)
    if (activeVariant === 'card') return [];

    // Helper for random selection
    const getRandom = (arr: string[], count: number) => {
        // Simple shuffle
        const shuffled = [...arr].sort(() => 0.5 - Math.random());
        return shuffled.slice(0, count);
    };

    if (activeCard?.type === 'timer') {
      const pool = [
        "开始25分钟专注", "开始10分钟休息", "设置专注目标",
        "开启番茄钟", "休息一下", "专注工作",
        "设定45分钟倒计时", "提醒我喝水"
      ];
      return getRandom(pool, 3);
    }
    if (activeCard?.type === 'alarm') {
        const pool = [
            "明天早上7点叫我", "取消所有闹钟", "查看我的闹钟",
            "设定闹钟", "叫醒我", "关闭闹钟",
            "提醒我起床", "设置工作日闹钟"
        ];
        return getRandom(pool, 3);
    }
    if (activeCard?.type === 'weather') {
        const pool = [
            "明天天气怎么样", "未来一周天气", "今天需要带伞吗",
            "北京天气", "气温多少度", "会有雨吗",
            "穿什么衣服合适", "空气质量如何"
        ];
        return getRandom(pool, 3);
    }

    if (activeCard?.type === 'podcast') {
        const pool = [
            "播放", "暂停", "下一首",
            "上一首", "讲了什么", "换一个",
            "退出播客", "有什么好听的"
        ];
        return getRandom(pool, 3);
    }

    if (activeCard?.type === 'chat') {
        const pool = [
            "我最近有点累", "讲个笑话开心下", "想找人说说话",
            "今天工作不顺心", "陪我聊聊心事", "最近压力好大",
            "分享一件开心的事", "你觉得我怎么样"
        ];
        return getRandom(pool, 3);
    }
    
    // Default fallback
    const defaultPool = [
        "讲个故事", "明天天气", "陪我聊聊天",
        "唱首歌", "讲个笑话", "新闻播报",
        "几点了", "打开专注模式"
    ];
    return getRandom(defaultPool, 3);

  }, [activeCard?.id, activeCard?.type, timerStatus, isTimerActive, dynamicSuggestions, suggestionError]);

  if (activeVariant === 'card') {
    return (
      <div className="w-full flex flex-col items-center gap-4">
        {/* Mic Button Area */}
        <div className="relative flex items-center justify-center h-16">
           <AnimatePresence mode="wait">
             {isListening ? (
               <motion.div
                 key="card-listening"
                 initial={{ width: 64, opacity: 0 }}
                 animate={{ width: 'auto', opacity: 1 }}
                 exit={{ width: 64, opacity: 0 }}
                 className={`flex items-center gap-4 px-8 py-4 bg-white rounded-full shadow-xl border border-orange-100`}
               >
                  {/* Waveform */}
                  <div className="flex gap-1 items-center h-6">
                    {[1,2,3,4,5].map(i => (
                      <motion.div 
                        key={i}
                        animate={{ height: [8, 24, 8] }}
                        transition={{ duration: 0.5, repeat: Infinity, delay: i * 0.1 }}
                        className="w-1.5 bg-orange-500 rounded-full"
                      />
                    ))}
                  </div>
                  <span className="text-orange-500 font-bold text-sm whitespace-nowrap">请说话...</span>
               </motion.div>
             ) : isThinking ? (
               <motion.div
                 key="card-thinking"
                 initial={{ scale: 0.8, opacity: 0 }}
                 animate={{ scale: 1, opacity: 1 }}
                 exit={{ scale: 0.8, opacity: 0 }}
                 className={`flex items-center gap-3 px-6 py-3 ${theme.colors.cardBg} rounded-full border ${theme.colors.cardBorder} shadow-md`}
               >
                  <Command className={`w-5 h-5 ${theme.colors.accent} animate-spin-slow`} />
                  <span className={`text-[13px] font-bold uppercase tracking-widest ${theme.colors.accent}`}>思考中</span>
               </motion.div>
             ) : isSpeaking ? (
               <motion.div
                 key="card-speaking"
                 initial={{ scale: 0.8, opacity: 0 }}
                 animate={{ scale: 1, opacity: 1 }}
                 exit={{ scale: 0.8, opacity: 0 }}
                 className={`flex items-center gap-3 px-6 py-3 ${theme.colors.cardBg} rounded-full border ${theme.colors.cardBorder} shadow-md`}
               >
                  <div className={`w-2.5 h-2.5 rounded-full ${theme.colors.accentBg} animate-pulse`} />
                  <span className={`text-[13px] font-bold uppercase tracking-widest ${theme.colors.textSecondary}`}>回复中</span>
               </motion.div>
             ) : (
               <motion.button
                 key="card-idle"
                 initial={{ scale: 0.8, opacity: 0 }}
                 animate={{ scale: 1, opacity: 1 }}
                 exit={{ scale: 0.8, opacity: 0 }}
                 whileHover={{ scale: 1.05 }}
                 whileTap={{ scale: 0.95 }}
                 onClick={onStartInteraction}
                 className={`w-16 h-16 rounded-full ${theme.colors.cardBg} border ${theme.colors.cardBorder} flex items-center justify-center shadow-md hover:shadow-lg transition-all duration-300`}
               >
                 <Mic className={`w-7 h-7 ${theme.colors.accent}`} />
               </motion.button>
             )}
           </AnimatePresence>
        </div>

        {/* Suggestions Area */}
        <div className="flex flex-wrap justify-center gap-2 w-full px-2">
          {suggestions.map(txt => (
            <button 
              key={txt}
              disabled={suggestionError || isThinking || isSpeaking}
              onClick={(e) => {
                  if (suggestionError || isThinking || isSpeaking) return;
                  e.stopPropagation();
                  onSimulatedSpeechEnd(txt);
              }}
              className={`px-3 py-1.5 rounded-full ${theme.colors.cardBg}/80 backdrop-blur-sm border ${theme.colors.cardBorder} text-[11px] font-medium 
                ${(suggestionError || isThinking || isSpeaking)
                    ? 'text-slate-400 cursor-not-allowed bg-slate-100/50 opacity-60' 
                    : `${theme.colors.textSecondary} hover:${theme.colors.accentBg}/10 hover:${theme.colors.accent} cursor-pointer shadow-sm`
                } transition-all duration-300`}
            >
              {txt}
            </button>
          ))}
        </div>
      </div>
    );
  }

  if (activeVariant === 'mini') {
    return (
      <div className="w-full flex flex-col items-center gap-4">
        <AnimatePresence mode="wait">
          {(!isListening && !isThinking && !isSpeaking) ? (
            <motion.button
              key="mini-idle"
              initial={{ opacity: 0, scale: 0.8 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.8 }}
              onClick={onStartInteraction}
              className={`w-14 h-14 rounded-full ${theme.colors.cardBg} border ${theme.colors.cardBorder} flex items-center justify-center shadow-md group z-10 hover:shadow-lg transition-all duration-500`}
            >
              <Mic className={`w-6 h-6 ${theme.colors.accent} group-hover:scale-110 transition-transform`} />
            </motion.button>
          ) : (
            <motion.div
              key="mini-active"
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: 10 }}
              className={`flex items-center gap-3 px-5 py-3 ${theme.colors.cardBg} rounded-full border ${theme.colors.cardBorder} shadow-md transition-colors duration-500`}
            >
              {isListening && (
                <div className="flex gap-1.5 items-center">
                  {[1,2,3,4].map(i => (
                    <motion.div 
                      key={i}
                      animate={{ height: [6, 16, 6] }}
                      transition={{ duration: 0.5, repeat: Infinity, delay: i * 0.1 }}
                      className={`w-1.5 ${theme.colors.accentBg} rounded-full`}
                    />
                  ))}
                </div>
              )}
              {isThinking && <Command className={`w-5 h-5 ${theme.colors.accent} animate-spin-slow`} />}
              {isSpeaking && <div className={`w-2.5 h-2.5 rounded-full ${theme.colors.accentBg} animate-pulse`} />}
              <span className={`text-[11px] font-bold uppercase tracking-widest ${theme.colors.textMuted}`}>
                {isListening ? "Listening" : isThinking ? "Thinking" : "Speaking"}
              </span>
            </motion.div>
          )}
        </AnimatePresence>
        
        {/* Always show suggestions in mini mode if listening, ensure z-index */}
        {isListening && (
          <div className="flex flex-nowrap justify-center gap-2 relative z-20 w-full max-w-[260px] overflow-hidden px-1">
            {suggestions.map(txt => (
              <button 
                key={txt}
                disabled={suggestionError}
                onClick={(e) => {
                    if (suggestionError) return;
                    e.stopPropagation();
                    onSimulatedSpeechEnd(txt);
                }}
                className={`flex-1 min-w-0 px-3 py-1.5 rounded-full ${theme.colors.cardBg} border ${theme.colors.cardBorder} text-[11px] font-medium 
                    ${suggestionError 
                        ? 'text-slate-400 cursor-not-allowed bg-slate-100/50' 
                        : `${theme.colors.textSecondary} hover:${theme.colors.accentBg}/10 hover:${theme.colors.accent} cursor-pointer shadow-sm`
                    } transition-colors truncate text-center`}
                title={txt}
              >
                {txt}
              </button>
            ))}
          </div>
        )}
      </div>
    );
  }

  return (
    <div className="w-full max-w-2xl flex flex-col items-center justify-center relative min-h-[160px] -mt-8">
      
      <AnimatePresence mode="wait">
        
        {/* STATE: IDLE or TIMER ACTIVE (but not listening/thinking/speaking) */}
        {(!isListening && !isThinking && !isSpeaking) && (
          <motion.div
            key="idle"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.9 }}
            className="flex flex-col items-center gap-6"
          >
            <motion.button
              whileHover={{ scale: 1.05, y: -2 }}
              whileTap={{ scale: 0.95, y: 0 }}
              onClick={onStartInteraction}
              className={`relative w-24 h-24 rounded-full bg-gradient-to-tr ${theme.colors.robotAuraOuter} flex items-center justify-center group shadow-[0_10px_30px_-10px_rgba(249,115,22,0.5)] overflow-hidden transition-colors duration-500`}
            >
              <motion.div 
                animate={{ scale: [1, 1.4], opacity: [0.2, 0] }}
                transition={{ duration: 2, repeat: Infinity }}
                className="absolute inset-0 rounded-full border-2 border-white/50"
              />
              <div className="absolute inset-0 bg-gradient-to-b from-white/20 to-transparent" />
              <div className={`w-16 h-16 rounded-full ${theme.colors.cardBg} flex items-center justify-center shadow-inner border border-slate-50 transition-colors duration-500`}>
                <Mic className={`w-8 h-8 ${theme.colors.accent} z-10 group-hover:scale-110 transition-transform`} />
              </div>
            </motion.button>
            
            <div className="flex flex-col items-center gap-2">
               <motion.div 
                 initial={{ opacity: 0 }}
                 animate={{ opacity: 1 }}
                 className={`px-6 py-2.5 ${theme.colors.cardBg} backdrop-blur-md rounded-full flex items-center gap-2 border ${theme.colors.cardBorder} shadow-sm transition-colors duration-500`}
               >
                  <MessageSquareText className={`w-3.5 h-3.5 ${theme.colors.accent}`} />
                  <span className={`text-[12px] ${theme.colors.textSecondary} font-bold uppercase tracking-widest`}>
                    {isTimerActive ? "点击说话控制" : "叫名字，开始对话"}
                  </span>
               </motion.div>
            </div>
          </motion.div>
        )}

        {/* STATE: ACTIVE ROBOT (LISTENING / THINKING / SPEAKING) */}
        {(isListening || isThinking || isSpeaking) && (
          <motion.div
            key="active-status"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -20 }}
            className="flex flex-col items-center gap-6 w-full"
          >
             {/* Status Badge & Mic Animation */}
             <div className={`flex items-center gap-4 ${theme.colors.cardBg} backdrop-blur-xl px-8 py-4 rounded-full border ${theme.colors.cardBorder} shadow-md transition-colors duration-500`}>
                {isListening && (
                   <div className="flex items-center gap-4">
                      <div className="flex gap-1.5 items-center">
                        {[1,2,3,4,5].map(i => (
                          <motion.div 
                            key={i}
                            animate={{ height: [8, 24, 8] }}
                            transition={{ duration: 0.5, repeat: Infinity, delay: i * 0.1 }}
                            className={`w-1.5 ${theme.colors.accentBg} rounded-full`}
                          />
                        ))}
                      </div>
                      <span className={`text-[14px] font-bold uppercase tracking-widest ${theme.colors.accent}`}>请说话...</span>
                   </div>
                )}
                {isThinking && (
                   <div className="flex items-center gap-3">
                      <Command className={`w-5 h-5 ${theme.colors.accent} animate-spin-slow`} />
                      <span className={`text-[14px] font-bold uppercase tracking-widest ${theme.colors.accent}`}>思考中</span>
                   </div>
                )}
                {isSpeaking && (
                   <div className="flex items-center gap-4">
                      <div className="flex gap-1 items-center">
                        {[1,2,3].map(i => (
                          <motion.div 
                            key={i}
                            animate={{ scale: [1, 1.5, 1], opacity: [0.4, 1, 0.4] }}
                            transition={{ duration: 0.8, repeat: Infinity, delay: i * 0.2 }}
                            className={`w-2 h-2 ${theme.colors.accentBg} rounded-full`}
                          />
                        ))}
                      </div>
                      <span className={`text-[14px] font-bold uppercase tracking-widest ${theme.colors.textSecondary}`}>正在播报回复</span>
                   </div>
                )}
             </div>

             {/* Quick Simulation Trigger */}
             {isListening && (
               <div className="flex gap-3">
                  {suggestions.map(txt => (
                    <button 
                      key={txt}
                      disabled={suggestionError}
                      onClick={() => {
                          if (suggestionError) return;
                          onSimulatedSpeechEnd(txt);
                      }}
                      className={`px-4 py-1.5 rounded-full ${theme.colors.cardBg} border ${theme.colors.cardBorder} text-[11px] font-medium 
                        ${suggestionError 
                            ? 'text-slate-400 cursor-not-allowed bg-slate-100/50' 
                            : `${theme.colors.textSecondary} hover:${theme.colors.accentBg}/10 transition-colors hover:${theme.colors.accent} shadow-sm`
                        }`}
                    >
                      "{txt}"
                    </button>
                  ))}
               </div>
             )}
          </motion.div>
        )}

      </AnimatePresence>
    </div>
  );
};

export default VoiceInputPanel;
