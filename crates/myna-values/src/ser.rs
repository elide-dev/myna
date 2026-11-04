use crate::*;
use serde::ser::{self, Serialize};

pub(crate) struct Serializer<'a, A: Allocator + Clone> {
    pub doc: &'a mut Document<A>,
    pub depth: usize,
}
impl<'a, A: Allocator + Clone> Serializer<'a, A> {
    fn scalar(self, tag: u32, bits: u64) -> Result<(), Error> {
        self.doc.push(tag, 0, bits).map(|_| ())
    }
    fn compound(self, tag: u32) -> Result<Compound<'a, A>, Error> {
        if self.depth >= self.doc.limits.max_depth {
            return Err(Error::Limit);
        }
        let index = self.doc.push(tag, 0, 0)?;
        Ok(Compound {
            doc: self.doc,
            depth: self.depth + 1,
            index,
            count: 0,
            pending: false,
            outer: None,
        })
    }
    fn variant(self, name: &str, tag: u32) -> Result<Compound<'a, A>, Error> {
        let mut outer = self.compound(OBJECT)?;
        outer.doc.string(name)?;
        outer.pending = true;
        let mut inner = Serializer {
            doc: outer.doc,
            depth: outer.depth,
        }
        .compound(tag)?;
        inner.outer = Some(outer.index);
        Ok(inner)
    }
}

macro_rules! signed {
    ($($name:ident: $ty:ty),*) => {$(
        fn $name(self, value: $ty) -> Result<(), Error> { self.serialize_i64(value as i64) }
    )*};
}
macro_rules! unsigned {
    ($($name:ident: $ty:ty),*) => {$(
        fn $name(self, value: $ty) -> Result<(), Error> { self.serialize_u64(value as u64) }
    )*};
}
impl<'a, A: Allocator + Clone> ser::Serializer for Serializer<'a, A> {
    type Ok = ();
    type Error = Error;
    type SerializeSeq = Compound<'a, A>;
    type SerializeTuple = Compound<'a, A>;
    type SerializeTupleStruct = Compound<'a, A>;
    type SerializeTupleVariant = Compound<'a, A>;
    type SerializeMap = Compound<'a, A>;
    type SerializeStruct = Compound<'a, A>;
    type SerializeStructVariant = Compound<'a, A>;

    signed!(serialize_i8: i8, serialize_i16: i16, serialize_i32: i32);
    unsigned!(serialize_u8: u8, serialize_u16: u16, serialize_u32: u32);
    fn serialize_bool(self, value: bool) -> Result<(), Error> {
        self.scalar(BOOL, u64::from(value))
    }
    fn serialize_i64(self, value: i64) -> Result<(), Error> {
        self.scalar(I64, value as u64)
    }
    fn serialize_u64(self, value: u64) -> Result<(), Error> {
        self.scalar(U64, value)
    }
    fn serialize_i128(self, value: i128) -> Result<(), Error> {
        if let Ok(v) = i64::try_from(value) {
            self.serialize_i64(v)
        } else {
            self.serialize_u64(u64::try_from(value).map_err(|_| Error::IntegerRange)?)
        }
    }
    fn serialize_u128(self, value: u128) -> Result<(), Error> {
        self.serialize_u64(u64::try_from(value).map_err(|_| Error::IntegerRange)?)
    }
    fn serialize_f32(self, value: f32) -> Result<(), Error> {
        self.serialize_f64(f64::from(value))
    }
    fn serialize_f64(self, value: f64) -> Result<(), Error> {
        if !value.is_finite() {
            return Err(Error::NonFinite);
        }
        self.scalar(F64, value.to_bits())
    }
    fn serialize_char(self, value: char) -> Result<(), Error> {
        self.serialize_str(value.encode_utf8(&mut [0; 4]))
    }
    fn serialize_str(self, value: &str) -> Result<(), Error> {
        self.doc.string(value)
    }
    fn serialize_bytes(self, value: &[u8]) -> Result<(), Error> {
        let mut array = self.compound(ARRAY)?;
        for byte in value {
            array.element(byte)?;
        }
        array.finish()
    }
    fn serialize_none(self) -> Result<(), Error> {
        self.serialize_unit()
    }
    fn serialize_some<T: Serialize + ?Sized>(self, value: &T) -> Result<(), Error> {
        value.serialize(self)
    }
    fn serialize_unit(self) -> Result<(), Error> {
        self.scalar(NULL, 0)
    }
    fn serialize_unit_struct(self, _: &'static str) -> Result<(), Error> {
        self.serialize_unit()
    }
    fn serialize_unit_variant(
        self,
        _: &'static str,
        _: u32,
        variant: &'static str,
    ) -> Result<(), Error> {
        self.serialize_str(variant)
    }
    fn serialize_newtype_struct<T: Serialize + ?Sized>(
        self,
        _: &'static str,
        value: &T,
    ) -> Result<(), Error> {
        value.serialize(self)
    }
    fn serialize_newtype_variant<T: Serialize + ?Sized>(
        self,
        _: &'static str,
        _: u32,
        variant: &'static str,
        value: &T,
    ) -> Result<(), Error> {
        let mut object = self.compound(OBJECT)?;
        object.field(variant, value)?;
        object.finish()
    }
    fn serialize_seq(self, _: Option<usize>) -> Result<Self::SerializeSeq, Error> {
        self.compound(ARRAY)
    }
    fn serialize_tuple(self, _: usize) -> Result<Self::SerializeTuple, Error> {
        self.compound(ARRAY)
    }
    fn serialize_tuple_struct(
        self,
        _: &'static str,
        _: usize,
    ) -> Result<Self::SerializeTupleStruct, Error> {
        self.compound(ARRAY)
    }
    fn serialize_tuple_variant(
        self,
        _: &'static str,
        _: u32,
        variant: &'static str,
        _: usize,
    ) -> Result<Self::SerializeTupleVariant, Error> {
        self.variant(variant, ARRAY)
    }
    fn serialize_map(self, _: Option<usize>) -> Result<Self::SerializeMap, Error> {
        self.compound(OBJECT)
    }
    fn serialize_struct(self, _: &'static str, _: usize) -> Result<Self::SerializeStruct, Error> {
        self.compound(OBJECT)
    }
    fn serialize_struct_variant(
        self,
        _: &'static str,
        _: u32,
        variant: &'static str,
        _: usize,
    ) -> Result<Self::SerializeStructVariant, Error> {
        self.variant(variant, OBJECT)
    }
    fn collect_str<T: fmt::Display + ?Sized>(self, value: &T) -> Result<(), Error> {
        struct Writer<'a, A: Allocator + Clone> {
            doc: &'a mut Document<A>,
            small: [u8; INLINE_STRING_BYTES],
            len: usize,
            error: Option<Error>,
        }
        impl<A: Allocator + Clone> fmt::Write for Writer<'_, A> {
            fn write_str(&mut self, text: &str) -> fmt::Result {
                if self.error.is_some() {
                    return Err(fmt::Error);
                }
                let result = (|| {
                    let end = self.len.checked_add(text.len()).ok_or(Error::Limit)?;
                    if end > u32::MAX as usize {
                        return Err(Error::Limit);
                    }
                    self.doc.charge_utf8(text.len())?;
                    if end <= INLINE_STRING_BYTES {
                        self.small[self.len..end].copy_from_slice(text.as_bytes());
                    } else {
                        let prefix = if self.len <= INLINE_STRING_BYTES {
                            self.len
                        } else {
                            0
                        };
                        self.doc
                            .bytes
                            .try_reserve(prefix + text.len())
                            .map_err(|_| Error::Allocation)?;
                        if prefix != 0 {
                            self.doc.append_reserved_bytes(&self.small[..prefix]);
                        }
                        self.doc.append_reserved_bytes(text.as_bytes());
                    }
                    self.len = end;
                    Ok(())
                })();
                if let Err(error) = result {
                    self.error = Some(error);
                    return Err(fmt::Error);
                }
                Ok(())
            }
        }
        let start = self.doc.bytes.len();
        if start > u32::MAX as usize {
            return Err(Error::Limit);
        }
        let index = self.doc.push(STRING | REGION_TEXT, 0, start as u64)?;
        let mut writer = Writer {
            doc: self.doc,
            small: [0; INLINE_STRING_BYTES],
            len: 0,
            error: None,
        };
        let result = fmt::write(&mut writer, format_args!("{value}"));
        if let Some(error) = writer.error {
            return Err(error);
        }
        result.map_err(|_| Error::Protocol("Display formatting failed"))?;
        writer.doc.nodes[index].len = writer.len as u32;
        if writer.len <= INLINE_STRING_BYTES {
            writer.doc.nodes[index].tag = STRING;
            writer.doc.nodes[index].set_inline(&writer.small[..writer.len]);
        }
        Ok(())
    }
    // JSON-shaped semantics also govern types with custom human-readable forms.
    fn is_human_readable(&self) -> bool {
        true
    }
}

pub(crate) struct Compound<'a, A: Allocator + Clone> {
    doc: &'a mut Document<A>,
    depth: usize,
    index: usize,
    count: u32,
    pending: bool,
    outer: Option<usize>,
}
impl<A: Allocator + Clone> Compound<'_, A> {
    fn child<T: Serialize + ?Sized>(&mut self, value: &T) -> Result<(), Error> {
        let start = self.doc.nodes.len();
        let result = value.serialize(Serializer {
            doc: self.doc,
            depth: self.depth,
        });
        if let Err(error) = result {
            self.doc.failed = true;
            return Err(error);
        }
        if self
            .doc
            .nodes
            .get(start)
            .is_none_or(|node| node.subtree_end(start) != self.doc.nodes.len() as u64)
        {
            self.doc.failed = true;
            return Err(Error::Protocol("expected exactly one value"));
        }
        Ok(())
    }
    fn element<T: Serialize + ?Sized>(&mut self, value: &T) -> Result<(), Error> {
        self.child(value)?;
        self.count = self.count.checked_add(1).ok_or(Error::Limit)?;
        Ok(())
    }
    fn key<T: Serialize + ?Sized>(&mut self, key: &T) -> Result<(), Error> {
        if self.pending {
            self.doc.failed = true;
            return Err(Error::Protocol("key without previous value"));
        }
        let index = self.doc.nodes.len();
        self.child(key)?;
        if self.doc.nodes[index].kind() != STRING {
            self.doc.failed = true;
            return Err(Error::KeyType);
        }
        self.pending = true;
        Ok(())
    }
    fn value<T: Serialize + ?Sized>(&mut self, value: &T) -> Result<(), Error> {
        if !self.pending {
            self.doc.failed = true;
            return Err(Error::Protocol("value without key"));
        }
        self.element(value)?;
        self.pending = false;
        Ok(())
    }
    fn field<T: Serialize + ?Sized>(&mut self, key: &'static str, value: &T) -> Result<(), Error> {
        self.key(key)?;
        self.value(value)
    }
    fn finish(self) -> Result<(), Error> {
        if self.doc.failed {
            return Err(Error::Protocol("a previous container operation failed"));
        }
        if self.pending {
            self.doc.failed = true;
            return Err(Error::Protocol("object ended without value"));
        }
        self.doc.finish(self.index, self.count);
        if let Some(outer) = self.outer {
            self.doc.finish(outer, 1);
        }
        Ok(())
    }
}
macro_rules! sequence {
    ($trait:ident, $method:ident) => {
        impl<A: Allocator + Clone> ser::$trait for Compound<'_, A> {
            type Ok = ();
            type Error = Error;
            fn $method<T: Serialize + ?Sized>(&mut self, value: &T) -> Result<(), Error> {
                self.element(value)
            }
            fn end(self) -> Result<(), Error> {
                self.finish()
            }
        }
    };
}
sequence!(SerializeSeq, serialize_element);
sequence!(SerializeTuple, serialize_element);
sequence!(SerializeTupleStruct, serialize_field);
sequence!(SerializeTupleVariant, serialize_field);
impl<A: Allocator + Clone> ser::SerializeMap for Compound<'_, A> {
    type Ok = ();
    type Error = Error;
    fn serialize_key<T: Serialize + ?Sized>(&mut self, key: &T) -> Result<(), Error> {
        self.key(key)
    }
    fn serialize_value<T: Serialize + ?Sized>(&mut self, value: &T) -> Result<(), Error> {
        self.value(value)
    }
    fn end(self) -> Result<(), Error> {
        self.finish()
    }
}
macro_rules! structure {
    ($trait:ident) => {
        impl<A: Allocator + Clone> ser::$trait for Compound<'_, A> {
            type Ok = ();
            type Error = Error;
            fn serialize_field<T: Serialize + ?Sized>(
                &mut self,
                key: &'static str,
                value: &T,
            ) -> Result<(), Error> {
                self.field(key, value)
            }
            fn end(self) -> Result<(), Error> {
                self.finish()
            }
        }
    };
}
structure!(SerializeStruct);
structure!(SerializeStructVariant);
