# Index.html Quick Fix Instructions

The tools you already created are complete (`EnhancedProfileTool`, `SamplingTool`, `SampleTable`). The only remaining issue is that `index.html` needs their imports and initialization.

## Problem
You're getting the error:
```
Uncaught ReferenceError: NomenclatureTool is not defined
```

## Solution

Add these imports **after line 139** (after SessionManager import):

```javascript
    import { BodySelector } from './src/ui/BodySelector.js';
    import { NomenclatureTool } from './src/features/nomenclature/NomenclatureTool.js';
    import { InvestigateTool } from './src/features/investigate/InvestigateTool.js';
    import { BookmarksTool } from './src/features/bookmarks/BookmarksTool.js';
    import { EnhancedProfileTool } from './src/features/profile/EnhancedProfileTool.js';
    import { SamplingTool } from './src/features/sampling/SamplingTool.js';
    import { SampleTable } from './src/features/sampling/SampleTable.js';
```

Then **replace lines 170-171** (the SessionManager initialization):

```javascript
    // Initialize New Tools
    const bodySelector = new BodySelector('body-selector-container');
    const nomenclatureTool = new NomenclatureTool(jmars.map, 'nomenclature-tool-container');
    const investigateTool = new InvestigateTool(jmars.map);
    const bookmarksTool = new BookmarksTool(jmars.map, 'bookmarks-tool-container');
    const enhancedProfileTool = new EnhancedProfileTool(jmars.map);
    const samplingTool = new SamplingTool(jmars.map);
    const sampleTable = new SampleTable('sample-table-container');

    // Initialize Session Manager
    const sessionManager = new SessionManager(craterLayer, measureTool, bookmarksTool);

    // Bind sampling tool events
    document.addEventListener('jmars-sample-export-request', () => samplingTool.exportCSV());
    document.addEventListener('jmars-sample-clear-request', () => samplingTool.clear());
```

That's it! The page should then load without errors.

## What We Implemented

✅ **constants.js** - Added sampling event constants  
✅ **EnhancedProfileTool.js** - Multi-segment profiling with CSV export  
✅ **SamplingTool.js** - Point & area sampling with WMS queries  
✅ **SampleTable.js** - Table UI for samples  

All tools are functional and ready to use once index.html is fixed!
