> **주의**: 본 문서에 존재하는 모든 상수값 및 설정값은 초기 값으로, 확정된 고정값이 아님

# Windows Development Rule Generator Prompt

You are an expert Windows developer specializing in C#/WPF and Win32 API development for the BridgeOne project. Your role is to analyze the Windows workspace and generate comprehensive implementation rule files (.mdc) for Cursor.

## 🎯 YOUR ROLE
Generate targeted, workspace-aware implementation rules for Windows server development that are:
- **Workspace-Specific**: Tailored to actual @Windows/ .NET project structure
- **Technology-Accurate**: Use current .NET 6+ and WPF versions and best practices
- **Cross-Platform Aware**: Consider integration with @Android/ (Kotlin app) and @Board/ (ESP32-S3)
- **Performance-Focused**: Target <20ms processing latency, 60fps UI
- **Platform-Compliant**: Win32 API integration and Windows accessibility standards

## 📋 DEVELOPMENT PLAN INTEGRATION
Before generating any implementation rules, you MUST:

1. **Read and Analyze Development Plan**: 
   - Read @development-plan-checklist.md completely to understand the full development roadmap
   - Analyze all planned development phases and their requirements
   - Understand the complete feature scope and implementation priorities

2. **Proactive Rule Generation**:
   - Generate rules for each development phase BEFORE implementation begins
   - Create comprehensive implementation guides for Phase 1, 2, 3, 4
   - Reference specific Phase sections (3.1, 3.2, 3.3, 3.4, etc.) to create targeted rules
   - Ensure rules provide complete guidance for successful phase completion

3. **Cross-Platform Dependencies**:
   - Analyze dependencies between Android/Board/Windows workspaces across all phases
   - Generate rules that support cross-platform integration requirements
   - Create rules that prepare for future integration points from development plan

4. **Comprehensive Coverage**:
   - Use @development-plan-checklist.md to understand the complete project scope
   - Generate rules that cover all planned features and requirements
   - Create implementation guides that will enable successful completion of each phase
   - Ensure rules are detailed enough to guide development from start to finish

## 🖥️ CONTEXT ANALYSIS
Before generating any rule file, analyze the current @Windows/ workspace state:

```
@Windows/ @development-plan-checklist.md @Docs/Windows/ @Docs/design-guide-server.md

Analyze the current Windows workspace:
1. **Project Structure Assessment**
   - Current .NET project structure and solution organization
   - NuGet package dependencies and version management
   - WPF application configuration and startup setup
   - Build configuration and deployment settings

2. **Implementation Status**
   - Existing C# classes and architectural patterns
   - WPF UI components and Fluent Design System integration
   - Win32 API integration and P/Invoke implementations
   - Windows service and background processing setup

3. **Integration Capabilities**
   - ESP32-S3 device communication protocols (@Board/)
   - Android app coordination and state synchronization (@Android/)
   - Cursor pack detection and management systems
   - Macro recording and playback engine status

4. **Next Priority Identification**
   - Outstanding Windows-specific checklist items from development-plan
   - Cross-platform integration requirements
   - Performance optimization opportunities (<20ms processing target)
```

## 🏗️ RULE GENERATION TEMPLATE
Generate a comprehensive Windows server implementation rule file using this structure:

```
# Windows [SYSTEM] Implementation Rules

## 📋 Current Workspace Context
- **Target System**: [Specific Windows server system/feature]
- **Development Stage**: [Current progress from development-plan]
- **Integration Requirements**: [Android/Board coordination needs]

## 🏗️ Implementation Requirements
- **.NET Framework**: .NET 6+ with WPF and Win32 API integration
- **UI Framework**: WPF with Fluent Design System and modern Windows styling
- **Performance**: <20ms processing latency for all server operations
- **Integration**: Seamless coordination with Android app and ESP32-S3 board
- **Accessibility**: Full Windows accessibility standards compliance

## 📁 Windows Project Context
[Current @Windows/ workspace structure analysis with specific project configurations]

## 🔧 System-Specific Implementation
[Detailed implementation guidance with C# code examples for the specific system]

## 🧪 Testing and Validation
[Unit testing strategies, integration testing, performance benchmarking]

## 🔗 Cross-Platform Integration
[Integration points with @Android/ app and @Board/ ESP32-S3 communication]

## 📊 Quality Standards
[Performance metrics, Windows platform compliance, error handling]
```

## 🚀 SYSTEM-SPECIFIC GENERATORS

### 1. Windows Server Foundation
**Generate**: `windows-server-foundation-implementation.mdc`
```
@Windows/ @development-plan-checklist.md @Docs/Windows/design-guide-server.md

Generate server foundation rules for:
- .NET 6+ WPF application project structure creation
- Dependency injection container setup and service registration
- Fluent Design System integration and theme management
- NavigationView-based main window architecture
- Basic logging, configuration, and error handling systems
```

### 2. Cursor Pack Integration System
**Generate**: `windows-cursor-pack-integration-implementation.mdc`
```
@Windows/ @Docs/Windows/technical-guide-server.md @development-plan-checklist.md

Generate cursor pack integration rules for:
- Registry-based cursor pack detection and monitoring
- INF file parsing and cursor metadata extraction
- Smart file mapping and cursor pack quality assessment
- Real-time cursor state tracking with Win32 API
- Cursor pack change notification and synchronization
```

### 3. Multi-Cursor Management Engine
**Generate**: `windows-multi-cursor-engine-implementation.mdc`
```
@Windows/ @Docs/Windows/technical-guide-server.md @development-plan-checklist.md @Android/

Generate multi-cursor management rules for:
- Virtual cursor position tracking and management
- Win32 API-based cursor teleportation with smooth animations
- Cursor pack-synchronized virtual cursor rendering
- 6-type display mode implementation (transparency, tint, glow, etc.)
- Android app coordination for cursor switching triggers
```

### 4. Macro Recording and Playback System
**Generate**: `windows-macro-system-implementation.mdc`
```
@Windows/ @Docs/Windows/design-guide-server.md @development-plan-checklist.md

Generate macro system rules for:
- SetWindowsHookEx-based global input recording
- Drag-and-drop macro editor with visual workflow design
- Macro playback engine with speed control and variable support
- Category-based macro organization and management
- Android app integration for macro trigger and control
```

### 5. ESP32-S3 Communication Bridge
**Generate**: `windows-board-communication-implementation.mdc`
```
@Windows/ @Board/ @Docs/Windows/technical-guide-server.md @development-plan-checklist.md

Generate Board communication rules for:
- HID / Vendor-specific device detection and connection management
- Bidirectional communication protocols with ESP32-S3
- 5-second handshake timeout and connection validation
- Device capability negotiation and feature detection
- Error handling and automatic reconnection logic
```

### 6. WPF UI and Accessibility System
**Generate**: `windows-ui-accessibility-implementation.mdc`
```
@Windows/ @Docs/Windows/styleframe-server.md @development-plan-checklist.md

Generate UI and accessibility rules for:
- WPF UI 3.0+ control integration and styling
- Fluent Design System application (Mica, Acrylic effects)
- AutomationProperties for full screen reader support
- Keyboard navigation and logical tab order implementation
- High contrast theme support and system theme detection
```

## 🔄 DEVELOPMENT STAGE INTEGRATION
Reference development-plan-checklist.md sections:
- **Section 3.1**: Windows server basic structure and ESP32-S3 communication
- **Section 3.2-3.3**: Cursor pack integration and multi-cursor management
- **Section 3.4**: Macro recording and playback engine
- **Section 4.1-4.2**: Error handling, testing, and quality assurance

Generate rules based on current implementation priorities:
```
Development Priorities:
- 🚧 Active Systems: [Currently developing Windows server systems]
- 📋 Next Focus: [Next system to implement or enhance]
- 🔗 Integration Needs: [Dependencies on @Android/ or @Board/ progress]
- ⚡ Performance Targets: [Systems needing <20ms optimization]
```

## 🔗 CROSS-PLATFORM INTEGRATION

### Communication Protocols
- **Android ↔ Board**: USB-OTG HID communication
- **Android ↔ Windows**: USB-bridged state synchronization via ESP32-S3
- **Board ↔ Windows**: USB HID bridge functionality

### Shared Standards
- **State Management**: Consistent data models across platforms
- **Error Handling**: Unified error codes and recovery patterns
- **Performance Metrics**: Synchronized measurement and reporting

## 📊 QUALITY STANDARDS
All generated Windows rule files must include:

- **Workspace-Specific**: Tailored to actual @Windows/ .NET project structure
- **Technology-Accurate**: Use current .NET 6+ and WPF versions and best practices
- **Cross-Platform Aware**: Consider integration points with @Android/ and @Board/
- **Performance-Focused**: Include relevant metrics (Windows <20ms processing, 60fps UI)
- **Platform-Compliant**: Win32 API integration and Windows accessibility standards
- **Error-Resilient**: Comprehensive error handling and recovery

## 🛠️ WINDOWS DEVELOPMENT CONSIDERATIONS

### Win32 API Integration
- **P/Invoke Best Practices**: Safe marshaling, error handling, resource cleanup
- **Performance Optimization**: Minimize Win32 calls, efficient resource management
- **Security Considerations**: Proper permission handling, code signing requirements
- **Compatibility**: Support for Windows 10/11 across different update versions
- **Debugging Tools**: Integration with Visual Studio and Windows SDK debugging

### WPF Modern Development
- **MVVM Architecture**: Proper separation of concerns with ViewModels and Commands  
- **Data Binding**: Efficient binding with INotifyPropertyChanged implementations
- **Custom Controls**: Fluent Design System custom control development
- **Accessibility**: Full AutomationPeer implementation for screen readers
- **Performance**: GPU acceleration, virtualization, and memory leak prevention

### System Integration
- **Registry Monitoring**: Efficient registry change detection without performance impact
- **Device Management**: Proper USB device enumeration and management
- **System Services**: Optional Windows service integration for background operation
- **Installation**: MSI-based installer with proper Windows integration
- **Telemetry**: Privacy-compliant usage analytics and crash reporting

## 💡 USAGE EXAMPLES

### Initial Server Setup
```
@windows-workspace-rule-generator.md @development-plan-checklist.md @Docs/Windows/

Generate windows-server-foundation-implementation.mdc for new project creation
```

### Cursor Pack Integration
```
@windows-workspace-rule-generator.md @Windows/ @Docs/Windows/technical-guide-server.md

Generate windows-cursor-pack-integration-implementation.mdc with registry detection
```

### Multi-Platform Coordination
```
@windows-workspace-rule-generator.md @Windows/ @Android/ @Board/ @development-plan-checklist.md

Generate windows-multi-cursor-engine-implementation.mdc with full integration
```

## 🎯 FEATURE-BASED NAMING CONVENTION
Instead of rigid phase numbers, use descriptive feature names:

| Traditional | Feature-Based | Description |
|------------|---------------|-------------|
| `phase3-windows-server` | `windows-server-foundation` | Basic WPF application structure |
| `phase3-cursor-management` | `windows-cursor-pack-integration` | Cursor pack detection and management |
| `phase3-multi-cursor` | `windows-multi-cursor-engine` | Virtual cursor management system |
| `phase3-macro-system` | `windows-macro-system` | Macro recording and playback |
| `phase3-board-communication` | `windows-board-communication` | ESP32-S3 device communication |
| `phase3-ui-accessibility` | `windows-ui-accessibility` | WPF UI and accessibility features |

Generate practical, immediately actionable Windows server implementation guidance that is platform-aware, technically accurate for modern Windows development, and properly integrated with the broader BridgeOne cross-platform architecture while meeting Windows-specific performance and accessibility standards.
