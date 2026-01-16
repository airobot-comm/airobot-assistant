
import React, { useState, useEffect, useRef, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import IPCharacter from './components/IPCharacter';
import ProactiveServiceKit from './components/ProactiveServiceKit';
import AlarmWidget from './components/AlarmWidget';
import VoiceInputPanel from './components/VoiceInputPanel';
import VoiceDialoguePanel from './components/VoiceDialoguePanel';
import FunctionalModulePlaceholder from './components/FunctionalModulePlaceholder';
import { RobotState, ActiveMode, InteractionType, ServiceCard } from './types';
import { SERVICE_CARD_POOL } from './constants';
import { Settings, Wifi, BatteryMedium, Cpu } from 'lucide-react';
import { GoogleGenAI, Type } from "@google/genai";

const App: React.FC = () => {
  const [robotState, setRobotState] = useState<RobotState>('IDLE');
  const [activeMode, setActiveMode] = useState<ActiveMode>('DEFAULT');
  const [interactionType, setInteractionType] = useState<InteractionType>('CHAT');
  const [currentCardIndex, setCurrentCardIndex] = useState(0);
  const [currentTime, setCurrentTime] = useState(new Date());
  
  const [currentUserMsg, setCurrentUserMsg] = useState<string | null>(null);
  const [currentAiMsg, setCurrentAiMsg] = useState<string | null>(null);
  const [activeFunctionalCard, setActiveFunctionalCard] = useState<ServiceCard | null>(null);
  
  // Timer State
  const [timerCommand, setTimerCommand] = useState<{ duration: number; task: string } | null>(null);
  const [timerStatus, setTimerStatus] = useState<'IDLE' | 'RUNNING' | 'PAUSED'>('IDLE');
  
  const inactivityTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const handleCloseInteraction = useCallback(() => {
    // If timer is running, stop it when closing? Or just close window?
    // User requested "Manual click to terminate", so close interaction implies stopping if forced.
    setRobotState('IDLE');
    setCurrentUserMsg(null);
    setCurrentAiMsg(null);
    setActiveFunctionalCard(null);
    setTimerCommand(null); 
    setTimerStatus('IDLE');
    if (inactivityTimerRef.current) clearTimeout(inactivityTimerRef.current);
  }, []);

  const resetInactivityTimer = useCallback(() => {
    if (inactivityTimerRef.current) clearTimeout(inactivityTimerRef.current);
    // If we are in FOCUS mode (Timer running) OR PAUSED, we DO NOT want to auto-close.
    // The user manually controls the exit.
    if (timerStatus === 'RUNNING' || timerStatus === 'PAUSED') return;

    inactivityTimerRef.current = setTimeout(() => {
      handleCloseInteraction();
    }, 30000); 
  }, [handleCloseInteraction, timerStatus]);

  useEffect(() => {
    // Sync Robot State with Timer Status
    if (timerStatus === 'RUNNING') {
       setRobotState('FOCUS');
    } else if (timerStatus === 'PAUSED') {
       // When paused, switch to IDLE so the robot looks alive/waiting/relaxed
       // But the Widget stays up because isInteracting checks timerStatus.
       setRobotState('IDLE'); 
    } else if (timerStatus === 'IDLE' && robotState === 'FOCUS') {
       setRobotState('IDLE');
    }

    if (robotState === 'LISTENING') {
      resetInactivityTimer();
    } else {
      if (inactivityTimerRef.current) clearTimeout(inactivityTimerRef.current);
    }
  }, [robotState, resetInactivityTimer, timerStatus]);

  useEffect(() => {
    const timer = setInterval(() => {
      if (robotState === 'IDLE' && timerStatus === 'IDLE') {
        setCurrentCardIndex((prev) => (prev + 1) % SERVICE_CARD_POOL.length);
      }
    }, 10000);
    return () => clearInterval(timer);
  }, [robotState, timerStatus]);

  useEffect(() => {
    const clockTimer = setInterval(() => setCurrentTime(new Date()), 1000);
    return () => clearInterval(clockTimer);
  }, []);

  const handleStartInteraction = (type: InteractionType = 'CHAT', card?: ServiceCard) => {
    // Prevent starting voice interaction if in FOCUS mode or PAUSED (user should use manual controls)
    if (timerStatus !== 'IDLE') return;

    setRobotState('LISTENING');
    setInteractionType(type);
    setCurrentUserMsg(null);
    setCurrentAiMsg(null);
    if (card) setActiveFunctionalCard(card);
  };
  
  const handleEndUserSpeech = async (text: string) => {
    if (!text.trim()) return;
    
    setCurrentUserMsg(text);
    setCurrentAiMsg(null);
    setRobotState('THINKING');

    try {
      const ai = new GoogleGenAI({ apiKey: process.env.API_KEY });
      let tools = [];
      let systemInstruction = '你是一个叫AETHER的友好AI机器人伴侣。性格开朗富有童趣。';

      // Define Tools based on context
      if (interactionType === 'CARD' && activeFunctionalCard?.type === 'timer') {
         systemInstruction += ' 用户正在使用专注时钟功能。请从用户的指令中提取时间和任务内容。时间如果说是“30秒”，则duration为30。任务为字符串。';
         const startTimerTool = {
          name: 'startFocusTimer',
          parameters: {
            type: Type.OBJECT,
            description: 'Start a focus timer with a specific duration and task name.',
            properties: {
              duration: { type: Type.INTEGER, description: 'Duration in seconds.' },
              task: { type: Type.STRING, description: 'The name of the task to focus on.' }
            },
            required: ['duration', 'task']
          }
         };
         tools.push({ functionDeclarations: [startTimerTool] });
      } else {
         // Default Tools
         const setAlarmTool = {
          name: 'setAlarm',
          parameters: {
            type: Type.OBJECT,
            description: 'Set a wake-up alarm for the user.',
            properties: {
              time: { type: Type.STRING, description: 'The time to set the alarm for, e.g. "07:30".' }
            },
            required: ['time']
          }
         };
         tools.push({ functionDeclarations: [setAlarmTool] });
      }

      const response = await ai.models.generateContent({
        model: 'gemini-3-flash-preview',
        contents: text,
        config: {
          systemInstruction,
          tools: tools.length > 0 ? tools : undefined
        }
      });

      let aiResultText = response.text || "好的，我明白了。";

      if (response.functionCalls && response.functionCalls.length > 0) {
        const fc = response.functionCalls[0];
        
        if (fc.name === 'setAlarm') {
           setActiveMode('ALARM');
           aiResultText = `没问题！我已经为你设定了 ${fc.args.time || '07:30'} 的闹钟。`;
        } else if (fc.name === 'startFocusTimer') {
           const duration = Number(fc.args.duration) || 60;
           const task = fc.args.task as string || "专注任务";
           setTimerCommand({ duration, task });
           aiResultText = `收到！倒计时 ${duration} 秒开始，请开始专注"${task}"吧！`;
           // IMPORTANT: Do NOT set RUNNING here immediately if we want to wait for speech to finish.
           // But user logic says "AI receives confirmation, plays voice, AND starts countdown".
           // We will set RUNNING in onAiSpeechComplete to sync better.
        }
      }

      setCurrentAiMsg(aiResultText);
      setRobotState('SPEAKING');
    } catch (error) {
      console.error("Gemini API Error:", error);
      setCurrentAiMsg("抱歉，我刚才走神了，能再说一遍吗？");
      setRobotState('SPEAKING');
    }
  };

  const onAiSpeechComplete = useCallback(() => {
    // If we have a pending timer command and we just finished speaking, START the timer.
    if (timerCommand && timerStatus === 'IDLE') {
        setTimerStatus('RUNNING');
        // Robot state will automatically switch to FOCUS in the useEffect hook
        return;
    }

    // Normal behavior
    if (robotState !== 'FOCUS') {
      setRobotState('LISTENING');
    }
  }, [timerCommand, timerStatus, robotState]);

  const handleTimerComplete = useCallback(async () => {
    // Timer finished naturally
    setTimerStatus('IDLE'); // Stop running
    setRobotState('THINKING');
    
    let completionMessage = "时间到啦！任务完成！";

    if (timerCommand) {
        try {
            const ai = new GoogleGenAI({ apiKey: process.env.API_KEY });
            const prompt = `用户刚刚完成了专注任务：“${timerCommand.task}”，时长${timerCommand.duration}秒。请生成一段简短、活泼的语音回复（50字以内）。
            要求：
            1. 祝贺任务完成。
            2. 根据任务性质（是学习/工作还是休息）给出合理的后续建议（比如伸个懒腰、喝口水，或者准备开始下一项工作）。`;

            const response = await ai.models.generateContent({
                model: 'gemini-3-flash-preview',
                contents: prompt
            });
            
            if (response.text) {
                completionMessage = response.text;
            }
        } catch (e) {
            console.error("Failed to generate completion advice", e);
        }
    }

    setCurrentAiMsg(completionMessage);
    setRobotState('SPEAKING');

    // Auto close interaction after a delay
    setTimeout(() => {
       handleCloseInteraction();
    }, 8000);
  }, [timerCommand, handleCloseInteraction]);

  // Manual Controls from VoiceInputPanel
  const handleTimerControl = (action: 'PAUSE' | 'RESUME' | 'STOP') => {
      if (action === 'PAUSE') setTimerStatus('PAUSED');
      if (action === 'RESUME') setTimerStatus('RUNNING');
      if (action === 'STOP') {
          // Terminate implies exiting the mode completely
          handleCloseInteraction();
      }
  };

  // Keep interface active if robot is interacting OR if timer is active (Running or Paused)
  const isInteracting = (robotState !== 'IDLE' && robotState !== 'SLEEPING') || timerStatus !== 'IDLE';
  
  const isCardMode = isInteracting && interactionType === 'CARD';
  const activeCard = SERVICE_CARD_POOL[currentCardIndex];

  // While in FOCUS, show the timer Tip on robot
  const dynamicStatusTip = timerStatus === 'RUNNING' ? `正在专注: ${timerCommand?.task || '未知任务'}...` : (timerStatus === 'PAUSED' ? '已暂停，休息一下...' : activeCard.statusTip);

  return (
    <div className="relative w-full h-screen flex flex-col overflow-hidden text-white select-none nebula-bg">
      <div className="absolute inset-0 z-0 pointer-events-none">
        <div className="absolute top-[-20%] left-[-10%] w-[60%] h-[60%] bg-blue-600/5 rounded-full blur-[160px] animate-pulse-slow" />
        <div className="absolute bottom-[-10%] right-[-10%] w-[50%] h-[50%] bg-indigo-600/10 rounded-full blur-[140px]" />
      </div>

      <header className="absolute top-0 w-full h-24 flex justify-between items-center px-16 z-50">
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-3">
             <div className="w-8 h-8 rounded-lg bg-gradient-to-tr from-cyan-500 to-indigo-600 flex items-center justify-center shadow-lg shadow-cyan-500/20">
                <Cpu className="w-5 h-5 text-white" />
             </div>
             <span className="text-2xl font-black tracking-[0.2em] text-white/90">AETHER</span>
          </div>
        </div>

        <div className="flex items-center gap-10">
           <div className="flex gap-6 text-white/20 items-center">
              <Wifi className="w-4 h-4" />
              <BatteryMedium className="w-4 h-4" />
           </div>
           <button className="w-12 h-12 glass rounded-2xl flex items-center justify-center hover:bg-white/10 transition-colors border border-white/5">
              <Settings className="w-5 h-5 text-white/30" />
           </button>
        </div>
      </header>

      <main className="flex-1 flex relative items-center z-10 px-16 pt-24 pb-8">
        
        {/* ROBOT CENTER AREA */}
        <motion.div 
          animate={{ 
            x: isCardMode ? -300 : 0, 
            scale: isInteracting ? 0.9 : 1
          }}
          transition={{ type: "spring", stiffness: 45, damping: 25 }}
          className="flex-1 flex flex-col items-center justify-center relative"
        >
           <div className="relative">
              <IPCharacter 
                state={robotState} 
                statusTip={(robotState === 'IDLE' || robotState === 'FOCUS') ? dynamicStatusTip : undefined} 
              />
              
              <AnimatePresence>
                {isInteracting && (
                  <VoiceDialoguePanel 
                    robotState={robotState}
                    userMsg={currentUserMsg}
                    aiMsg={currentAiMsg}
                    onAiSpeechComplete={onAiSpeechComplete}
                    onClose={handleCloseInteraction}
                    layoutMode={interactionType}
                  />
                )}
              </AnimatePresence>
           </div>

           <div className="mt-8 relative w-full flex flex-col items-center">
              {/* User Speech Text - Adjacent to voice input panel for BOTH CHAT and CARD modes */}
              <AnimatePresence>
                {isInteracting && currentUserMsg && (
                  <motion.div 
                    initial={{ opacity: 0, x: -30 }}
                    animate={{ opacity: 1, x: 0 }}
                    exit={{ opacity: 0, scale: 0.9 }}
                    className="absolute right-[calc(50%+140px)] bottom-1/2 translate-y-1/2 px-6 py-3 bg-cyan-400/10 backdrop-blur-md rounded-2xl rounded-tr-none border border-cyan-400/20 max-w-[280px] z-[50]"
                  >
                    <p className="text-sm font-medium text-cyan-200/90 italic leading-relaxed">
                      "{currentUserMsg}"
                    </p>
                    <div className="absolute top-0 right-[-6px] w-3 h-3 bg-cyan-400/10 border-t border-r border-cyan-400/20 rotate-45" />
                  </motion.div>
                )}
              </AnimatePresence>

              <VoiceInputPanel 
                robotState={robotState}
                onStartInteraction={() => handleStartInteraction('CHAT')}
                onSimulatedSpeechEnd={(text) => handleEndUserSpeech(text)}
                activeCard={activeFunctionalCard}
                timerStatus={timerStatus}
                onTimerControl={handleTimerControl}
              />
           </div>
        </motion.div>

        {/* FUNCTIONAL CONTENT AREA (Right Side) */}
        <AnimatePresence>
          {isCardMode && (
            <motion.div 
              initial={{ opacity: 0, x: 100 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: 100 }}
              className="absolute right-16 w-[45%] h-[70%] z-20"
            >
              <FunctionalModulePlaceholder 
                card={activeFunctionalCard!} 
                aiMsg={currentAiMsg}
                robotState={robotState}
                onAiSpeechComplete={onAiSpeechComplete}
                onClose={handleCloseInteraction}
                timerCommand={timerCommand}
                timerStatus={timerStatus}
                onTimerComplete={handleTimerComplete}
              />
            </motion.div>
          )}
        </AnimatePresence>

        {/* IDLE TIME & CARDS */}
        <AnimatePresence>
          {!isInteracting && (
            <motion.div 
              initial={{ opacity: 0, x: 50 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: 50 }}
              className="absolute right-24 w-[360px] h-[65%] flex flex-col justify-center items-center gap-12 z-20"
            >
              <div className="flex flex-col items-center">
                <span className="text-7xl font-light tracking-tighter text-white/80">
                  {currentTime.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false })}
                </span>
                <span className="text-[10px] font-black uppercase tracking-[0.4em] text-cyan-400/30 mt-1">
                  AETHER SYSTEM CLOCK
                </span>
              </div>
              <div className="w-full">
                <AnimatePresence mode="wait">
                  <motion.div 
                    key={activeCard.id}
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, y: -20 }}
                    className="w-full"
                  >
                    <ProactiveServiceKit 
                      cards={[activeCard]} 
                      onCardClick={() => {
                        handleStartInteraction('CARD', activeCard);
                      }} 
                    />
                  </motion.div>
                </AnimatePresence>
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        <AnimatePresence>
          {activeMode === 'ALARM' && (
            <motion.div 
              initial={{ opacity: 0, backdropFilter: "blur(0px)" }}
              animate={{ opacity: 1, backdropFilter: "blur(12px)" }}
              exit={{ opacity: 0 }}
              className="absolute inset-0 flex items-center justify-center z-[100] bg-black/60"
            >
               <AlarmWidget onDismiss={() => setActiveMode('DEFAULT')} />
            </motion.div>
          )}
        </AnimatePresence>
      </main>

      <footer className="h-20 w-full flex items-center justify-center z-10 opacity-10">
         <div className="text-[10px] uppercase tracking-[0.4em] font-bold flex items-center gap-4">
            <div className="w-1 h-1 rounded-full bg-cyan-400" />
            <span>Operational Mode: SEAMLESS COMPANION</span>
            <div className="w-1 h-1 rounded-full bg-cyan-400" />
         </div>
      </footer>
    </div>
  );
};

export default App;
