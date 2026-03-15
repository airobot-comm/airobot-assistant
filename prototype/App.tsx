
import React, { useState, useEffect, useRef, useCallback } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import IPCharacter from './components/IPCharacter';
import ProactiveServiceKit from './components/ProactiveServiceKit';
import AlarmWidget from './components/AlarmWidget';
import VoiceInputPanel from './components/VoiceInputPanel';
import VoiceDialoguePanel from './components/VoiceDialoguePanel';
import FunctionalModulePlaceholder from './components/FunctionalModulePlaceholder';
import ServiceStateSidebar, { MiniRobotAvatar } from './components/ServiceStateSidebar';
import { RobotState, ActiveMode, InteractionType, ServiceCard } from './types';
import { SERVICE_CARD_POOL, PODCASTS } from './constants';
import { Settings, Wifi, BatteryMedium, Cpu, Mic, MessageSquareText, Moon, Sun } from 'lucide-react';
import { GoogleGenAI, Type } from "@google/genai";
import { useTheme } from './theme';
import { KNOWLEDGE_CARDS } from './components/KnowledgeQuizWidget';

const App: React.FC = () => {
  const { theme, toggleTheme, mode } = useTheme();
  const [robotState, setRobotState] = useState<RobotState>('IDLE');
  const [activeMode, setActiveMode] = useState<ActiveMode>('DEFAULT');
  const [interactionType, setInteractionType] = useState<InteractionType>('CHAT');
  const [currentCardIndex, setCurrentCardIndex] = useState(0);
  const [currentTime, setCurrentTime] = useState(new Date());
  
  const [currentUserMsg, setCurrentUserMsg] = useState<string | null>(null);
  const [currentAiMsg, setCurrentAiMsg] = useState<string | null>(null);
  const [chatHistory, setChatHistory] = useState<{ role: 'user' | 'ai'; text: string }[]>([]);
  const [activeFunctionalCard, setActiveFunctionalCard] = useState<ServiceCard | null>(null);
  const [dynamicSuggestions, setDynamicSuggestions] = useState<{ text: string, targetCardId?: string }[]>([]);
  const [suggestionError, setSuggestionError] = useState(false);
  const [isDialogueOpen, setIsDialogueOpen] = useState(true);
  const [pendingExitService, setPendingExitService] = useState(false);
  const [resumePodcastAfterSpeech, setResumePodcastAfterSpeech] = useState(false);
  
  // Timer State
  const [timerCommand, setTimerCommand] = useState<{ duration: number; task: string } | null>(null);
  const [timerStatus, setTimerStatus] = useState<'IDLE' | 'RUNNING' | 'PAUSED'>('IDLE');

  // Quiz State
  const [currentQuizCardId, setCurrentQuizCardId] = useState<string>(KNOWLEDGE_CARDS[0].id);
  const [currentPodcastId, setCurrentPodcastId] = useState<string>(PODCASTS[0].id);
  const [podcastStatus, setPodcastStatus] = useState<'PLAYING' | 'PAUSED' | 'IDLE'>('IDLE');
  
  const inactivityTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const handleCloseInteraction = useCallback((forceClose: boolean = false) => {
    if (!forceClose) {
       // 所有功能卡片：关闭对话框仅收起界面转入等待状态，不退出服务
       if (activeFunctionalCard) {
           setIsDialogueOpen(false);
           setRobotState((timerStatus === 'RUNNING' || podcastStatus === 'PLAYING') ? 'FOCUS' : 'IDLE');
           return;
       }
       // 非功能卡片状态下，关闭对话框即退出
    }
    // 退出服务逻辑
    setRobotState('IDLE');
    setCurrentUserMsg(null);
    setCurrentAiMsg(null);
    setActiveFunctionalCard(null);
    setTimerCommand(null); 
    setTimerStatus('IDLE');
    setPodcastStatus('IDLE');
    setResumePodcastAfterSpeech(false);
    if ('speechSynthesis' in window) {
      window.speechSynthesis.cancel();
    }
    setIsDialogueOpen(true); // reset for next time
    setSuggestionError(false);
    setDynamicSuggestions([]);
    setPendingExitService(false);
    if (inactivityTimerRef.current) clearTimeout(inactivityTimerRef.current);
  }, [timerStatus, activeFunctionalCard, podcastStatus]);

  const resetInactivityTimer = useCallback(() => {
    if (inactivityTimerRef.current) clearTimeout(inactivityTimerRef.current);

    inactivityTimerRef.current = setTimeout(() => {
      // If in card mode, just close dialogue (waiting state). Otherwise, fully exit.
      handleCloseInteraction(false);
    }, 30000); 
  }, [handleCloseInteraction]);

  useEffect(() => {
    if (robotState === 'LISTENING') {
      resetInactivityTimer();
    } else {
      if (inactivityTimerRef.current) clearTimeout(inactivityTimerRef.current);
    }
  }, [robotState, resetInactivityTimer]);

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

  const handleStartInteraction = async (type: InteractionType = 'CHAT', card?: ServiceCard) => {
    setIsDialogueOpen(true);
    setInteractionType(type);
    setCurrentUserMsg(null);
    setCurrentAiMsg(null);
    setChatHistory([]);
    setSuggestionError(false);
    setDynamicSuggestions([]);
    
    if (podcastStatus === 'PLAYING') {
      setPodcastStatus('PAUSED');
      setResumePodcastAfterSpeech(true);
    } else {
      setResumePodcastAfterSpeech(false);
    }
    
    if (card) {
      setActiveFunctionalCard(card);
      // Initial Greeting for Card Mode
      if (type === 'CARD') {
        setRobotState('THINKING');
        try {
          const ai = new GoogleGenAI({ apiKey: process.env.API_KEY });
          let prompt = `用户刚刚打开了名为"${card.title}"的功能服务（类型：${card.type}）。
请生成一句简短、活泼的开场白（20字以内），以及3个用户可以直接对你说的语音指令建议（每个建议不超过8个字）。`;
          
          if (card.type === 'quiz') {
            const currentCard = KNOWLEDGE_CARDS.find(c => c.id === currentQuizCardId) || KNOWLEDGE_CARDS[0];
            prompt += `\n当前显示的知识卡片是关于“${currentCard.title}”的。开场白请提及这个主题，建议指令可以包括“换一张”、“关于${currentCard.title}的问题”等。`;
          } else if (card.type === 'podcast') {
            const currentPodcast = PODCASTS.find(p => p.id === currentPodcastId) || PODCASTS[0];
            prompt += `\n当前正在播放的播客是“${currentPodcast.title}”。开场白请提及这个播客，建议指令必须包含一个关于该播客内容的具体疑问句（例如：“艾瑟是谁？”或“黑洞是什么？”），以及“暂停播放”、“下一首”等控制指令。`;
          }

          prompt += `\n返回JSON格式：
{
  "greeting": "开场白",
  "suggestions": ["指令1", "指令2", "指令3"]
}`;
          const apiCall = ai.models.generateContent({
            model: 'gemini-3-flash-preview',
            contents: prompt,
            config: {
              responseMimeType: "application/json",
              responseSchema: {
                type: Type.OBJECT,
                properties: {
                  greeting: { type: Type.STRING },
                  suggestions: { type: Type.ARRAY, items: { type: Type.STRING } }
                },
                required: ["greeting", "suggestions"]
              }
            }
          });

          const timeout = new Promise((_, reject) => 
            setTimeout(() => reject(new Error("Timeout")), 30000)
          );

          const response = await Promise.race([apiCall, timeout]) as any;
          
          const data = JSON.parse(response.text);
          setCurrentAiMsg(data.greeting);
          setChatHistory([{ role: 'ai', text: data.greeting }]);
          setDynamicSuggestions(data.suggestions.map((s: string) => ({ text: s })));
          setRobotState('SPEAKING');
        } catch (e) {
          console.error("Failed to generate greeting", e);
          const fallbackGreeting = `抱歉，网络似乎有点慢。但我已经为你准备好了${card.title}服务，你可以直接对我说指令，或者点击左上角退出。`;
          setCurrentAiMsg(fallbackGreeting);
          setChatHistory([{ role: 'ai', text: fallbackGreeting }]);
          setDynamicSuggestions([]);
          setSuggestionError(true);
          setRobotState('SPEAKING');
        }
      } else {
        setRobotState('LISTENING');
      }
    } else {
      setRobotState('LISTENING');
      // Generate dynamic suggestions for CHAT mode
      const randomCards = [...SERVICE_CARD_POOL].sort(() => 0.5 - Math.random()).slice(0, 2);
      
      // Set immediate fallback suggestions initially, but clear if error occurs later
      setDynamicSuggestions([
        { text: `打开${randomCards[0].title}`, targetCardId: randomCards[0].id },
        { text: `打开${randomCards[1].title}`, targetCardId: randomCards[1].id },
        { text: "讲个笑话吧" }
      ]);

      // Async generate smarter suggestions
      const generateChatSuggestions = async () => {
        try {
          const ai = new GoogleGenAI({ apiKey: process.env.API_KEY });
          const prompt = `你是一个智能机器人AETHER。用户刚刚唤醒了你准备语音聊天。
请根据以下两个功能卡片，生成3个简短的语音指令建议（每个不超过8个字）。
卡片1：ID="${randomCards[0].id}", 名称="${randomCards[0].title}"
卡片2：ID="${randomCards[1].id}", 名称="${randomCards[1].title}"

要求：
1. 前2个建议必须是自然地表达想要使用这两个功能卡片的意图（例如对于“专注时钟”，建议可以是“帮我计时”或“我要专注”）。
2. 第3个建议是随意的日常聊天话题（如“讲个笑话”、“陪我聊天”、“今天心情如何”等），这个建议应该根据你作为AI助手的身份以及当前的功能背景自动算法生成。
返回JSON格式：
{
  "suggestions": [
    { "text": "指令1", "targetCardId": "卡片1的ID" },
    { "text": "指令2", "targetCardId": "卡片2的ID" },
    { "text": "指令3", "targetCardId": null }
  ]
}`;
          const response = await ai.models.generateContent({
            model: 'gemini-3-flash-preview',
            contents: prompt,
            config: {
              responseMimeType: "application/json",
              responseSchema: {
                type: Type.OBJECT,
                properties: {
                  suggestions: { 
                    type: Type.ARRAY, 
                    items: { 
                      type: Type.OBJECT,
                      properties: {
                        text: { type: Type.STRING },
                        targetCardId: { type: Type.STRING, nullable: true }
                      },
                      required: ["text"]
                    } 
                  }
                },
                required: ["suggestions"]
              }
            }
          });
          const data = JSON.parse(response.text);
          setDynamicSuggestions(data.suggestions);
        } catch (e) {
          console.error("Failed to generate smart chat suggestions", e);
          // If AI fails, we want to show "No response" instead of potentially wrong fallbacks
          setDynamicSuggestions([]);
          setSuggestionError(true);
        }
      };
      
      generateChatSuggestions();
    }
  };
  
  const handleEndUserSpeech = async (text: string) => {
    if (!text.trim()) return;

    if (interactionType === 'CHAT') {
      // 1. Check if the text matches a dynamic suggestion with a targetCardId
      const matchedSuggestion = dynamicSuggestions.find(s => s.text === text && s.targetCardId);
      if (matchedSuggestion) {
        const card = SERVICE_CARD_POOL.find(c => c.id === matchedSuggestion.targetCardId);
        if (card) {
          handleStartInteraction('CARD', card);
          return;
        }
      }
      
      // 2. Fallback regex matching
      const matchedCard = SERVICE_CARD_POOL.find(c => text.includes(c.title) && (text.includes('打开') || text.includes('进入') || text === `打开${c.title}`));
      if (matchedCard) {
        handleStartInteraction('CARD', matchedCard);
        return;
      }
    }
    
    setCurrentUserMsg(text);
    setChatHistory(prev => [...prev, { role: 'user', text }]);
    setCurrentAiMsg(null);
    setRobotState('THINKING');

    try {
      const ai = new GoogleGenAI({ apiKey: process.env.API_KEY });
      let tools = [];
      let systemInstruction = '你是一个叫AETHER的友好AI机器人伴侣。性格开朗富有童趣。';

      // Define Tools based on context
      if (interactionType === 'CARD') {
         systemInstruction += ` 用户正在使用${activeFunctionalCard?.title}功能。你可以执行该功能的相关操作，或者在用户要求退出时退出该功能。`;
         const exitServiceTool = {
          name: 'exitService',
          parameters: {
            type: Type.OBJECT,
            description: 'Exit the current functional service card and return to the home screen.',
            properties: {},
            required: []
          }
         };
         
         if (activeFunctionalCard?.type === 'timer') {
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
            const controlTimerTool = {
             name: 'controlTimer',
             parameters: {
               type: Type.OBJECT,
               description: 'Control the active focus timer (pause, resume, stop).',
               properties: {
                 action: { type: Type.STRING, enum: ['PAUSE', 'RESUME', 'STOP'], description: 'The action to perform.' }
               },
               required: ['action']
             }
            };
            tools.push({ functionDeclarations: [startTimerTool, controlTimerTool, exitServiceTool] });
         } else if (activeFunctionalCard?.type === 'quiz') {
            const currentCard = KNOWLEDGE_CARDS.find(c => c.id === currentQuizCardId) || KNOWLEDGE_CARDS[0];
            systemInstruction += ` 当前正在展示的知识卡片是：${currentCard.title}。你可以回答用户关于这张卡片的问题。如果用户说“换一张”、“下一张”等，你必须调用 switchQuizCard 工具来切换卡片。`;
            const switchQuizCardTool = {
             name: 'switchQuizCard',
             parameters: {
               type: Type.OBJECT,
               description: 'Switch to a different knowledge quiz card when the user asks to change it (e.g., "换一张", "下一张", "上一张", "随机").',
               properties: {
                 action: { type: Type.STRING, enum: ['NEXT', 'PREV', 'RANDOM'], description: 'The action to perform.' }
               },
               required: ['action']
             }
            };
            tools.push({ functionDeclarations: [switchQuizCardTool, exitServiceTool] });
         } else if (activeFunctionalCard?.type === 'podcast') {
            const currentPodcast = PODCASTS.find(p => p.id === currentPodcastId) || PODCASTS[0];
            systemInstruction += ` 当前正在播放的播客是：${currentPodcast.title}。播客内容如下：\n${currentPodcast.content}\n\n你可以回答用户关于这个播客内容的问题。如果用户要求播放、暂停、上一首、下一首，你必须调用 controlPodcast 工具。`;
            const controlPodcastTool = {
             name: 'controlPodcast',
             parameters: {
               type: Type.OBJECT,
               description: 'Control the interactive podcast (play, pause, next, prev).',
               properties: {
                 action: { type: Type.STRING, enum: ['PLAY', 'PAUSE', 'NEXT', 'PREV'], description: 'The action to perform.' }
               },
               required: ['action']
             }
            };
            tools.push({ functionDeclarations: [controlPodcastTool, exitServiceTool] });
         } else {
            tools.push({ functionDeclarations: [exitServiceTool] });
         }
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

      // Wrap API call in a timeout promise
      const apiCall = ai.models.generateContent({
        model: 'gemini-3-flash-preview',
        contents: text,
        config: {
          systemInstruction,
          tools: tools.length > 0 ? tools : undefined
        }
      });

      const timeout = new Promise((_, reject) => 
        setTimeout(() => reject(new Error("Timeout")), 30000)
      );

      const response = await Promise.race([apiCall, timeout]) as any;

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
        } else if (fc.name === 'controlTimer') {
           const action = fc.args.action as string;
           if (action === 'PAUSE') {
             setTimerStatus('PAUSED');
             aiResultText = '已经为你暂停了时钟。';
           } else if (action === 'RESUME') {
             setTimerStatus('RUNNING');
             aiResultText = '时钟已继续，加油！';
           } else if (action === 'STOP') {
             setTimerStatus('IDLE');
             setTimerCommand(null);
             aiResultText = '好的，时钟已结束。';
           }
        } else if (fc.name === 'switchQuizCard') {
          const action = (fc.args.action as string || '').toUpperCase();
          const currentIndex = KNOWLEDGE_CARDS.findIndex(c => c.id === currentQuizCardId);
          let newIndex = currentIndex;
          
          if (action === 'PREV') {
            newIndex = currentIndex === 0 ? KNOWLEDGE_CARDS.length - 1 : currentIndex - 1;
          } else if (action === 'RANDOM') {
            newIndex = Math.floor(Math.random() * KNOWLEDGE_CARDS.length);
          } else {
            // Default to NEXT
            newIndex = currentIndex === KNOWLEDGE_CARDS.length - 1 ? 0 : currentIndex + 1;
          }
          
          const newCard = KNOWLEDGE_CARDS[newIndex];
          setCurrentQuizCardId(newCard.id);
          aiResultText = `好的，我们来看看下一张卡片：${newCard.title}。`;
        } else if (fc.name === 'controlPodcast') {
          const action = (fc.args.action as string || '').toUpperCase();
          const currentIndex = PODCASTS.findIndex(p => p.id === currentPodcastId);
          
          if (action === 'PLAY' || action === 'RESUME') {
            setPodcastStatus('PLAYING');
            setResumePodcastAfterSpeech(false);
            aiResultText = '好的，继续播放。';
          } else if (action === 'PAUSE' || action === 'STOP') {
            setPodcastStatus('PAUSED');
            setResumePodcastAfterSpeech(false);
            aiResultText = '好的，已暂停。';
          } else if (action === 'NEXT') {
            const newIndex = currentIndex === PODCASTS.length - 1 ? 0 : currentIndex + 1;
            setCurrentPodcastId(PODCASTS[newIndex].id);
            setPodcastStatus('PLAYING');
            setResumePodcastAfterSpeech(false);
            aiResultText = `好的，为你播放下一首：${PODCASTS[newIndex].title}。`;
          } else if (action === 'PREV') {
            const newIndex = currentIndex === 0 ? PODCASTS.length - 1 : currentIndex - 1;
            setCurrentPodcastId(PODCASTS[newIndex].id);
            setPodcastStatus('PLAYING');
            setResumePodcastAfterSpeech(false);
            aiResultText = `好的，为你播放上一首：${PODCASTS[newIndex].title}。`;
          }
        } else if (fc.name === 'exitService') {
           aiResultText = '好的，已为你退出服务。';
           setPendingExitService(true);
        }
      }

      setCurrentAiMsg(aiResultText);
      setChatHistory(prev => [...prev, { role: 'ai', text: aiResultText }]);
      setRobotState('SPEAKING');
    } catch (error) {
      console.error("Gemini API Error:", error);
      const errorMsg = "服务器暂无响应，请稍后再试。";
      setCurrentAiMsg(errorMsg);
      setChatHistory(prev => [...prev, { role: 'ai', text: errorMsg }]);
      setRobotState('SPEAKING');
    }
  };

  const onAiSpeechComplete = useCallback(() => {
    if (pendingExitService) {
        setPendingExitService(false);
        handleCloseInteraction(true);
        return;
    }

    // If we have a pending timer command and we just finished speaking, START the timer.
    if (timerCommand && timerStatus === 'IDLE') {
        setTimerStatus('RUNNING');
        // We want to continue listening for commands like "Stop" or "Pause"
        setRobotState('LISTENING'); 
        return;
    }

    if (resumePodcastAfterSpeech) {
        setPodcastStatus('PLAYING');
        setResumePodcastAfterSpeech(false);
        setRobotState('FOCUS');
        setIsDialogueOpen(false);
        return;
    }

    if (podcastStatus === 'PLAYING') {
        setRobotState('FOCUS');
        setIsDialogueOpen(false);
        return;
    }

    // Always return to a state where the user can interact again
    // Even if in FOCUS mode, we want to allow voice commands (pause, stop, etc.)
    // So we should go back to LISTENING.
    setRobotState('LISTENING');
  }, [timerCommand, timerStatus, podcastStatus, robotState, pendingExitService, handleCloseInteraction, resumePodcastAfterSpeech]);

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

    // Auto close dialogue box after a delay, but keep the timer card visible
    setTimeout(() => {
       setIsDialogueOpen(false);
       setRobotState('IDLE');
    }, 8000);
  }, [timerCommand]);

  // Manual Controls from VoiceInputPanel
  const handleTimerControl = (action: 'PAUSE' | 'RESUME' | 'STOP') => {
      if (action === 'PAUSE') setTimerStatus('PAUSED');
      if (action === 'RESUME') setTimerStatus('RUNNING');
      if (action === 'STOP') {
          // Terminate implies exiting the mode completely
          handleCloseInteraction(true);
      }
  };

  // Keep interface active if robot is interacting OR if timer is active (Running or Paused)
  const isInteracting = (robotState !== 'IDLE' && robotState !== 'SLEEPING') || timerStatus !== 'IDLE';
  
  const isCardMode = activeFunctionalCard !== null;
  const activeCard = SERVICE_CARD_POOL[currentCardIndex];

  // While in FOCUS, show the timer Tip on robot
  const dynamicStatusTip = timerStatus === 'RUNNING' ? `正在专注: ${timerCommand?.task || '未知任务'}...` : (timerStatus === 'PAUSED' ? '已暂停，休息一下...' : activeCard.statusTip);

  return (
    <div className={`relative w-full h-screen flex flex-col overflow-hidden ${theme.colors.textPrimary} select-none ${theme.colors.background} transition-colors duration-500`}>
      
      {/* Background Ambience - Geometric Shapes & Subtle Bubbles */}
      <div className="absolute inset-0 z-0 pointer-events-none overflow-hidden">
        {/* Large Geometric Circles (Reference Image Style) */}
        <div className={`absolute -top-[18vh] -left-[12vh] w-[55vh] h-[55vh] rounded-full ${theme.colors.backgroundShapes} transition-colors duration-500`} />
        <div className={`absolute -bottom-[12vh] -right-[7vh] w-[32vh] h-[32vh] rounded-full ${theme.colors.backgroundShapes} transition-colors duration-500`} />
        
        {/* Subtle Decorative Bubbles */}
        <motion.div 
          className={`absolute top-[40%] left-[15%] w-16 h-16 rounded-full ${theme.colors.bubbleBg} shadow-sm backdrop-blur-sm border ${theme.colors.bubbleBorder} transition-colors duration-500`}
          animate={{ y: [0, -20, 0], x: [0, 10, 0] }}
          transition={{ duration: 8, repeat: Infinity, ease: "easeInOut" }}
        />
      </div>

      <header className="absolute top-0 w-full h-24 flex justify-between items-center px-16 z-50">
        <div className="flex items-center gap-3">
           <div className={`w-10 h-10 rounded-2xl bg-gradient-to-tr ${theme.colors.robotAuraOuter} flex items-center justify-center shadow-lg`}>
              <Cpu className="w-6 h-6 text-white" />
           </div>
           <span className={`text-2xl font-black tracking-widest ${theme.colors.textPrimary}`}>AETHER</span>
        </div>

        <div className="flex items-center gap-10">
           {/* Right Side Status Area - Connectivity & Clock (Separated & Enlarged) */}
           <div className="flex items-center gap-2.5">
             <BatteryMedium className="w-5 h-5 text-emerald-500" />
             <span className={`text-xs font-black tracking-wider ${theme.colors.textMuted}`}>85%</span>
           </div>
           <div className="flex items-center gap-2.5">
             <Wifi className="w-5 h-5 text-emerald-500" />
             <span className={`text-xs font-black tracking-wider ${theme.colors.textMuted}`}>ONLINE</span>
           </div>
           <div className="flex items-center">
              <span className={`text-2xl font-bold tracking-widest ${theme.colors.textPrimary} font-mono leading-none`}>
                {currentTime.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', hour12: false })}
              </span>
           </div>
        </div>
      </header>

      <main className="flex-1 flex relative items-center z-10 px-16 pt-24 pb-8">
        
        {/* ROBOT CENTER AREA (Hidden in Card Mode) */}
        <AnimatePresence>
          {!isCardMode && (
            <motion.div 
              initial={{ opacity: 0, scale: 0.9 }}
              animate={{ opacity: 1, scale: isInteracting ? 0.9 : 1, y: 110 }}
              exit={{ opacity: 0, scale: 0.9 }}
              transition={{ type: "spring", stiffness: 45, damping: 25 }}
              className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none pb-[5vh]"
            >
              <div className="pointer-events-auto relative flex flex-col items-center w-full">
                  <IPCharacter 
                    state={robotState} 
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

              <div className="mt-8 relative w-full flex flex-col items-center pointer-events-auto">
                  {/* User Speech Text - Adjacent to voice input panel for CHAT mode */}
                  <AnimatePresence>
                    {isInteracting && currentUserMsg && (
                      <motion.div 
                        initial={{ opacity: 0, x: -30 }}
                        animate={{ opacity: 1, x: 0 }}
                        exit={{ opacity: 0, scale: 0.9 }}
                        className="absolute right-[calc(50%+140px)] bottom-1/2 translate-y-1/2 px-6 py-3 bg-white shadow-sm rounded-2xl rounded-tr-none border border-slate-100 max-w-[280px] z-[50]"
                      >
                        <p className="text-sm font-medium text-slate-600 leading-relaxed">
                          "{currentUserMsg}"
                        </p>
                        <div className="absolute top-0 right-[-6px] w-3 h-3 bg-white border-t border-r border-slate-100 rotate-45" />
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
                    dynamicSuggestions={dynamicSuggestions.map(s => s.text)}
                    suggestionError={suggestionError}
                  />
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* FUNCTIONAL CONTENT AREA (Center in Card Mode) */}
        <AnimatePresence>
          {isCardMode && (
            <motion.div 
              initial={{ opacity: 0, scale: 0.8 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.8 }}
              transition={{ type: "spring", stiffness: 50, damping: 20 }}
              className="absolute inset-0 flex items-center justify-center z-20 pointer-events-none"
            >
              <div className="w-[660px] h-[600px] pointer-events-auto">
                <FunctionalModulePlaceholder 
                  card={activeFunctionalCard!} 
                  aiMsg={currentAiMsg}
                  robotState={robotState}
                  onAiSpeechComplete={onAiSpeechComplete}
                  onClose={handleCloseInteraction}
                  timerCommand={timerCommand}
                  timerStatus={timerStatus}
                  onTimerComplete={handleTimerComplete}
                  onTimerControl={handleTimerControl}
                  currentQuizCardId={currentQuizCardId}
                  onQuizCardChange={setCurrentQuizCardId}
                  currentPodcastId={currentPodcastId}
                  podcastStatus={podcastStatus}
                  onPodcastControl={(action) => {
                    if (action === 'PLAY') setPodcastStatus('PLAYING');
                    else if (action === 'PAUSE') setPodcastStatus('PAUSED');
                    else if (action === 'NEXT') {
                      const currentIndex = PODCASTS.findIndex(p => p.id === currentPodcastId);
                      const newIndex = currentIndex === PODCASTS.length - 1 ? 0 : currentIndex + 1;
                      setCurrentPodcastId(PODCASTS[newIndex].id);
                      setPodcastStatus('PLAYING');
                    }
                    else if (action === 'PREV') {
                      const currentIndex = PODCASTS.findIndex(p => p.id === currentPodcastId);
                      const newIndex = currentIndex === 0 ? PODCASTS.length - 1 : currentIndex - 1;
                      setCurrentPodcastId(PODCASTS[newIndex].id);
                      setPodcastStatus('PLAYING');
                    }
                  }}
                />
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* SERVICE STATE SIDEBAR (Right side in Card Mode) */}
        <AnimatePresence>
          {isCardMode && isDialogueOpen && (
            <motion.div 
              initial={{ opacity: 0, x: 30 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: 30 }}
              transition={{ delay: 0.2 }}
              className="absolute right-[8%] top-[calc(20%+80px)] w-[300px] h-auto z-30 flex flex-col gap-8 pointer-events-none"
            >
              <div className="h-[370px] shrink-0 pointer-events-auto">
                <ServiceStateSidebar 
                  robotState={robotState}
                  chatHistory={chatHistory}
                  onAiSpeechComplete={onAiSpeechComplete}
                  onClose={() => handleCloseInteraction(true)}
                  activeCard={activeFunctionalCard}
                />
              </div>

              <div className="h-auto shrink-0 pointer-events-auto">
                <VoiceInputPanel 
                  robotState={robotState}
                  onStartInteraction={() => {
                    setRobotState('LISTENING');
                    if (podcastStatus === 'PLAYING') {
                      setPodcastStatus('PAUSED');
                      setResumePodcastAfterSpeech(true);
                    } else {
                      setResumePodcastAfterSpeech(false);
                    }
                  }}
                  onSimulatedSpeechEnd={(text) => handleEndUserSpeech(text)}
                  activeCard={activeFunctionalCard}
                  timerStatus={timerStatus}
                  onTimerControl={handleTimerControl}
                  variant="card"
                  dynamicSuggestions={dynamicSuggestions.map(s => s.text)}
                  suggestionError={suggestionError}
                />
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* MINI AETHER INTERACTION COMPONENT (Waiting State) */}
        <AnimatePresence>
          {isCardMode && !isDialogueOpen && (
            <motion.div 
              initial={{ opacity: 0, scale: 0.8, y: 20 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.8, y: 20 }}
              className="absolute right-[8%] bottom-16 z-30 cursor-pointer group flex flex-col items-center gap-3"
              onClick={() => {
                setIsDialogueOpen(true);
                setRobotState('LISTENING');
              }}
            >
              <div className="relative">
                {/* Enhanced glowing aura for better contrast */}
                <motion.div 
                  animate={{ scale: [1, 1.25, 1], opacity: [0.4, 0.8, 0.4] }}
                  transition={{ duration: 2.5, repeat: Infinity, ease: "easeInOut" }}
                  className="absolute inset-[-8px] rounded-[36px] bg-gradient-to-tr from-cyan-400 via-blue-500 to-purple-500 blur-xl opacity-50"
                />
                {/* Secondary pulse */}
                <motion.div 
                  animate={{ scale: [1, 1.1, 1], opacity: [0.6, 1, 0.6] }}
                  transition={{ duration: 1.5, repeat: Infinity, ease: "easeInOut", delay: 0.5 }}
                  className="absolute inset-[-2px] rounded-[30px] bg-gradient-to-tr from-cyan-300 to-blue-400 blur-md"
                />
                
                <MiniRobotAvatar robotState={robotState} className="w-16 h-16 rounded-[28px] shadow-2xl relative z-10 group-hover:scale-105 transition-transform duration-300 ring-2 ring-white/30" />
              </div>

              {/* Voice input indicator (animation only) */}
              <div className="flex items-center justify-center h-6 gap-1 mt-1">
                {[1, 2, 3, 4, 5].map((i) => (
                  <motion.div
                    key={i}
                    animate={{ 
                      height: [4, 12 + (i % 3) * 4, 4],
                      opacity: [0.5, 1, 0.5]
                    }}
                    transition={{ 
                      duration: 0.8 + (i % 2) * 0.3, 
                      repeat: Infinity, 
                      ease: "easeInOut",
                      delay: i * 0.1
                    }}
                    className="w-1 rounded-full bg-cyan-400 shadow-[0_0_8px_rgba(34,211,238,0.8)]"
                  />
                ))}
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* IDLE TIME & CARDS */}
        <AnimatePresence>
          {!isInteracting && !isCardMode && (
            <motion.div 
              initial={{ opacity: 0, x: 50 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: 50 }}
              className="absolute right-24 w-[345px] h-[65%] flex flex-col justify-center items-center z-20"
            >
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
                      headerTip={dynamicStatusTip}
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

      <footer className="h-20 w-full flex items-center justify-center z-10 opacity-20">
         <div className="text-[10px] uppercase tracking-[0.4em] font-bold flex items-center gap-4 text-slate-400">
            <div className="w-1 h-1 rounded-full bg-slate-300" />
            <span>AETHER COMPANION</span>
            <div className="w-1 h-1 rounded-full bg-slate-300" />
         </div>
      </footer>

      {/* Theme Toggle Button - Bottom Left */}
      <div className="absolute bottom-8 left-8 z-[100]">
        <button 
          onClick={toggleTheme}
          className={`w-14 h-14 rounded-full ${theme.colors.cardBg} border ${theme.colors.cardBorder} flex items-center justify-center shadow-lg hover:scale-105 active:scale-95 transition-all duration-300 backdrop-blur-md`}
        >
          {mode === 'light' ? <Moon className={`w-6 h-6 ${theme.colors.textSecondary}`} /> : <Sun className={`w-6 h-6 ${theme.colors.textSecondary}`} />}
        </button>
      </div>
    </div>
  );
};

export default App;
