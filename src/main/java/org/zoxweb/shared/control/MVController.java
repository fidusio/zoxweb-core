package org.zoxweb.shared.control;

import org.zoxweb.shared.util.SetValue;

public interface MVController<V>
extends SetValue<V>
{
	void render();
	
	void preSubmit();
	
	void submit();
	
	void postSubmit();
	
}
