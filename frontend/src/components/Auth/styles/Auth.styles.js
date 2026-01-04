import styled, { keyframes } from "styled-components";

// Keyframe animations
const float = keyframes`
    0%, 100% { transform: translateY(0px); }
    50% { transform: translateY(-6px); }
`;

const gradientShift = keyframes`
    0% { background-position: 0% 50%; }
    50% { background-position: 100% 50%; }
    100% { background-position: 0% 50%; }
`;

const fadeInUp = keyframes`
    from {
        opacity: 0;
        transform: translateY(12px);
    }
    to {
        opacity: 1;
        transform: translateY(0);
    }
`;

export const LoadingText = styled.div`
    color: #153243;
    font-size: 1.25rem;
    font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
    margin-bottom: 1.5rem;
    text-align: center;
`;

export const Spinner = styled.div`
    border: 4px solid rgba(15, 102, 204, 0.15);
    border-top: 4px solid #0b6cff;
    border-radius: 50%;
    width: 44px;
    height: 44px;
    animation: spin 0.9s linear infinite;
    margin: 0 auto;

    @keyframes spin {
        0% { transform: rotate(0deg); }
        100% { transform: rotate(360deg); }
    }
`;

export const LogoImg = styled.img`
    width: 110px;
    height: 110px;
    object-fit: contain;
    margin-bottom: 1.25rem;
    background: transparent;
    padding: 8px;
    border-radius: 12px;
    box-shadow: 0 6px 18px rgba(8, 25, 40, 0.06);
    animation: ${float} 4s ease-in-out infinite;
    transition: transform 0.18s ease;

    &:hover {
        transform: translateY(-4px);
    }

    @media (max-width: 500px) {
        width: 80px;
        height: 80px;
        margin-bottom: 0.9rem;
        padding: 6px;
        border-radius: 10px;
    }
`;

export const WelcomeHeading = styled.h1`
    color: #0b2b4a;
    font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
    font-weight: 700;
    font-size: 2rem;
    margin-bottom: 1.75rem;
    text-align: center;
    letter-spacing: -0.2px;
    animation: ${fadeInUp} 0.6s ease-out;

    @media (max-width: 500px) {
        font-size: 1.6rem;
        margin-bottom: 1rem;
    }
`;

export const GithubButton = styled.button`
    background: #0b6cff;
    color: #ffffff;
    border: none;
    border-radius: 10px;
    padding: 0.9rem 2rem;
    font-size: 1rem;
    font-weight: 600;
    font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
    cursor: pointer;
    display: flex;
    align-items: center;
    gap: 0.6rem;
    margin: 0 auto;
    transition: transform 0.15s ease, box-shadow 0.15s ease;
    position: relative;
    box-shadow: 0 6px 18px rgba(11, 44, 77, 0.12);

    &:hover {
        transform: translateY(-2px);
        box-shadow: 0 10px 26px rgba(11, 44, 77, 0.14);
        background: #095ad1;
    }

    &:active {
        transform: translateY(0);
    }

    @media (max-width: 500px) {
        padding: 0.75rem 1.5rem;
        font-size: 0.95rem;
    }
`;

export const GithubIcon = () => (
    <svg height="24" width="24" viewBox="0 0 24 24" fill="currentColor" style={{marginRight: 4}}>
        <path d="M12 .297c-6.63 0-12 5.373-12 12 0 5.303 3.438 9.8 8.205 11.387.6.113.82-.258.82-.577 0-.285-.01-1.04-.015-2.04-3.338.724-4.042-1.416-4.042-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.084-.729.084-.729 1.205.084 1.84 1.236 1.84 1.236 1.07 1.834 2.809 1.304 3.495.997.108-.775.418-1.305.762-1.605-2.665-.305-5.466-1.334-5.466-5.931 0-1.31.469-2.381 1.236-3.221-.124-.303-.535-1.523.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.553 3.297-1.23 3.297-1.23.653 1.653.242 2.873.119 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.803 5.624-5.475 5.921.43.372.823 1.102.823 2.222 0 1.606-.014 2.898-.014 3.293 0 .322.216.694.825.576C20.565 22.092 24 17.592 24 12.297c0-6.627-5.373-12-12-12"/>
    </svg>
);

export const AuthWrapper = styled.div`
    display: flex;
    height: 100vh;
    width: 100vw;
    justify-content: center;
    align-items: center;
    background: linear-gradient(135deg, #f5f6f9 0%, #eff2f7 100%);
    position: relative;
`;

export const LoginFormContainer = styled.div`
    max-width: 420px;
    width: 92vw;
    height: auto;
    min-height: 420px;
    border-radius: 12px;
    background: #fafbfc;
    border: 1px solid rgba(11, 44, 77, 0.06);
    box-shadow: 0 20px 40px rgba(11, 44, 77, 0.12), 0 2px 8px rgba(11, 44, 77, 0.08);
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    padding: 2.5rem 2rem;
    position: relative;
    animation: ${fadeInUp} 0.35s ease-out;

    &::before {
        content: '';
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        border-radius: 12px;
        pointer-events: none;
    }

    @media (max-width: 700px) {
        width: 360px;
        min-height: 380px;
        padding: 1.75rem 1.25rem;
        box-shadow: 0 16px 40px rgba(11, 44, 77, 0.1);
    }
`;