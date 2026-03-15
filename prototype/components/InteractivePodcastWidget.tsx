import React, { useState, useEffect, useRef } from 'react';
import { motion } from 'framer-motion';
import { Play, Pause, SkipForward, SkipBack, Volume2 } from 'lucide-react';
import { useTheme } from '../theme';
import { PODCASTS } from '../constants';

interface InteractivePodcastWidgetProps {
  currentPodcastId?: string;
  onPodcastChange?: (podcastId: string) => void;
  podcastStatus?: 'PLAYING' | 'PAUSED' | 'IDLE';
  onPodcastControl?: (action: 'PLAY' | 'PAUSE' | 'NEXT' | 'PREV') => void;
}

const InteractivePodcastWidget: React.FC<InteractivePodcastWidgetProps> = ({
  currentPodcastId,
  onPodcastChange,
  podcastStatus = 'IDLE',
  onPodcastControl
}) => {
  const { theme } = useTheme();
  const [progress, setProgress] = useState(0);
  const [currentTime, setCurrentTime] = useState(0);
  
  const currentPodcast = PODCASTS.find(p => p.id === currentPodcastId) || PODCASTS[0];
  // Estimate duration based on text length (approx 4 chars per second)
  const duration = Math.max(10, Math.ceil(currentPodcast.content.length / 4));

  const currentUtteranceId = useRef(0);

  const lastPodcastId = useRef(currentPodcastId);

  // Speech Synthesis for Podcast Audio
  useEffect(() => {
    if (!('speechSynthesis' in window)) return;
    
    const synth = window.speechSynthesis;
    
    // If podcast changed, cancel current speech
    if (lastPodcastId.current !== currentPodcastId) {
      currentUtteranceId.current += 1;
      synth.cancel();
      setCurrentTime(0);
      lastPodcastId.current = currentPodcastId;
    }
    
    if (podcastStatus === 'PLAYING') {
      if (synth.paused) {
        synth.resume();
      } else if (!synth.speaking) {
        currentUtteranceId.current += 1;
        const utteranceId = currentUtteranceId.current;
        const utterance = new SpeechSynthesisUtterance(currentPodcast.content);
        utterance.lang = 'zh-CN';
        utterance.rate = 0.9;
        utterance.pitch = currentPodcast.category === '故事' ? 1.2 : 1.0;
        
        utterance.onend = (e) => {
          if (utteranceId === currentUtteranceId.current) {
            onPodcastControl?.('NEXT');
          }
        };
        
        synth.speak(utterance);
      }
    } else if (podcastStatus === 'PAUSED') {
      if (synth.speaking && !synth.paused) {
        synth.pause();
      }
    } else if (podcastStatus === 'IDLE') {
      currentUtteranceId.current += 1;
      if (synth.paused) synth.resume();
      synth.cancel();
      setCurrentTime(0);
    }
  }, [podcastStatus, currentPodcast, currentPodcastId, onPodcastControl]);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      if ('speechSynthesis' in window) {
        currentUtteranceId.current += 1;
        const synth = window.speechSynthesis;
        if (synth.paused) synth.resume();
        synth.cancel();
      }
    };
  }, []);

  // Mock progress update
  useEffect(() => {
    let interval: ReturnType<typeof setInterval>;
    if (podcastStatus === 'PLAYING') {
      interval = setInterval(() => {
        setCurrentTime(prev => {
          if (prev >= duration) {
            onPodcastControl?.('NEXT');
            return 0;
          }
          return prev + 1;
        });
      }, 1000);
    }
    return () => clearInterval(interval);
  }, [podcastStatus, duration, onPodcastControl]);

  useEffect(() => {
    setProgress((currentTime / duration) * 100);
  }, [currentTime, duration]);

  const formatTime = (seconds: number) => {
    const m = Math.floor(seconds / 60);
    const s = Math.floor(seconds % 60);
    return `${m}:${s.toString().padStart(2, '0')}`;
  };

  return (
    <motion.div 
      className={`relative flex flex-col items-center justify-between w-[420px] h-[620px] rounded-[56px] shadow-[0_30px_60px_-15px_rgba(0,0,0,0.3),inset_0_0_0_1px_rgba(255,255,255,0.2)] overflow-hidden`}
      initial={{ scale: 0.9, opacity: 0, y: 20 }}
      animate={{ scale: 1, opacity: 1, y: 0 }}
      exit={{ scale: 0.9, opacity: 0, y: 20 }}
      transition={{ type: "spring", stiffness: 300, damping: 25 }}
    >
      {/* Background Image */}
      <div 
        className="absolute inset-0 bg-cover bg-center z-0"
        style={{ backgroundImage: `url(${currentPodcast.bgImage})` }}
      />
      
      {/* Gradient Overlay */}
      <div className="absolute inset-0 bg-gradient-to-b from-black/30 via-black/50 to-black/90 z-10" />

      {/* Content */}
      <div className="relative z-20 w-full h-full flex flex-col p-8 pt-12">
        {/* Header */}
        <div className="flex justify-between items-center w-full mb-auto">
          <span className="px-4 py-1.5 rounded-full bg-white/20 backdrop-blur-md text-white text-xs font-bold tracking-widest uppercase border border-white/10">
            {currentPodcast.category}
          </span>
          <Volume2 className="w-6 h-6 text-white/80" />
        </div>

        {/* Title & Info */}
        <div className="w-full text-left mb-8">
          <h2 className="text-3xl font-black text-white leading-tight mb-3 drop-shadow-lg">
            {currentPodcast.title}
          </h2>
          <p className="text-white/70 text-sm line-clamp-3 leading-relaxed">
            {currentPodcast.content}
          </p>
        </div>

        {/* Player Controls */}
        <div className="w-full flex flex-col gap-6 mb-4">
          {/* Progress Bar */}
          <div className="w-full flex flex-col gap-2">
            <div className="w-full h-2 bg-white/20 rounded-full overflow-hidden backdrop-blur-sm">
              <motion.div 
                className="h-full bg-white rounded-full"
                animate={{ width: `${progress}%` }}
                transition={{ duration: 0.5, ease: "linear" }}
              />
            </div>
            <div className="flex justify-between text-white/50 text-xs font-mono">
              <span>{formatTime(currentTime)}</span>
              <span>{formatTime(duration)}</span>
            </div>
          </div>

          {/* Buttons */}
          <div className="flex items-center justify-center gap-8">
            <button 
              onClick={() => onPodcastControl?.('PREV')}
              className="p-3 rounded-full hover:bg-white/10 transition-colors text-white"
            >
              <SkipBack className="w-8 h-8 fill-current" />
            </button>
            
            <button 
              onClick={() => onPodcastControl?.(podcastStatus === 'PLAYING' ? 'PAUSE' : 'PLAY')}
              className="p-6 rounded-full bg-white text-black hover:scale-105 active:scale-95 transition-all shadow-[0_0_30px_rgba(255,255,255,0.3)]"
            >
              {podcastStatus === 'PLAYING' ? (
                <Pause className="w-10 h-10 fill-current" />
              ) : (
                <Play className="w-10 h-10 fill-current ml-1" />
              )}
            </button>
            
            <button 
              onClick={() => onPodcastControl?.('NEXT')}
              className="p-3 rounded-full hover:bg-white/10 transition-colors text-white"
            >
              <SkipForward className="w-8 h-8 fill-current" />
            </button>
          </div>
        </div>
      </div>
    </motion.div>
  );
};

export default InteractivePodcastWidget;
